### PRE-REQUIREMENTS
# Running: Consul (localhost:8500)
# Running: EWMA Load Balancer (localhost:8777) - with enabled test-service backend

import threading
import time
import requests
import sys
from http.server import HTTPServer, BaseHTTPRequestHandler

LB_URL = "http://localhost:8777/proxy/test-service/some-comand"
CONSUL_URL = "http://localhost:8500/v1/agent/service/register"
CONSUL_DEREGISTER = "http://localhost:8500/v1/agent/service/deregister/"
FAST_BACKEND_PORT = 7081
SLOW_BACKEND_PORT = 7082
TOTAL_REQUESTS = 100

class PingHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/ping":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"OK")
        else:
            if 'slow' in self.server.backend_type:
                time.sleep(0.5)
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"pong")

def start_backend(port, backend_type):
    server = HTTPServer(("localhost", port), PingHandler)
    server.backend_type = backend_type
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    return server


def register_in_consul(service_id, name, port):
    payload = {
        "Name": name,
        "ID": service_id,
        "Address": "127.0.0.1",
        "Port": port,
        "Check": {
            "HTTP": f"http://127.0.0.1:{port}/ping",
            "Interval": "2s",
            "Timeout": "1000ms"
        }
    }
    resp = requests.put(CONSUL_URL, json=payload)
    assert resp.status_code == 200, f"Failed to register in Consul: {resp.text}"
    print(f"Registered {service_id} in Consul")


def deregister_from_consul(service_id):
    requests.put(CONSUL_DEREGISTER + service_id)


def send_requests():
    results = {"fast": 0, "slow": 0}
    instance_map = {}

    time.sleep(3)
    try:
        debug_resp = requests.get("http://localhost:8777/debug/instances")
        instances = debug_resp.json()["instances"]
        for inst in instances:
            if str(FAST_BACKEND_PORT) in inst["url"]:
                instance_map[inst["id"]] = "fast"
            elif str(SLOW_BACKEND_PORT) in inst["url"]:
                instance_map[inst["id"]] = "slow"
    except Exception as e:
        print("Could not fetch instance mapping:", e)
        return results

    input("Confirm that both services registered in EWMA: ")

    print(f"Sending {TOTAL_REQUESTS} requests to load balancer...")
    for i in range(TOTAL_REQUESTS):
        try:
            resp = requests.get(LB_URL, timeout=5)
            x_real_ip = resp.headers.get("X-Real-IP", "")
            debug_resp = requests.get("http://localhost:8777/debug/instances")
            instances = debug_resp.json()["instances"]
            selected = None
            for inst in instances:
                if inst["activeRequests"] > 0 or inst["totalRequests"] > 0:
                    selected = inst["id"]
                    break
            if selected and selected in instance_map:
                backend_type = instance_map[selected]
                results[backend_type] += 1
                print(f"Req {i+1}: routed to {backend_type} (instance {selected})")
            time.sleep(0.1)
        except Exception as e:
            print(f"Request failed: {e}")
    return results


def calculate_expected_ewma_weights(requests_per_backend):
    fast_weight = 1.0 / (5 + 1)
    slow_weight = 1.0 / (500 + 1)
    total_weight = fast_weight + slow_weight
    fast_ratio = fast_weight / total_weight
    slow_ratio = slow_weight / total_weight
    return fast_ratio, slow_ratio


def main():
    print("Starting EWMA behavior test...")

    fast_server = start_backend(FAST_BACKEND_PORT, "fast")
    slow_server = start_backend(SLOW_BACKEND_PORT, "slow")
    print("Backends started")

    register_in_consul("test-fast-1", "test-service", FAST_BACKEND_PORT)
    register_in_consul("test-slow-1", "test-service", SLOW_BACKEND_PORT)

    try:
        results = send_requests()

        total = results["fast"] + results["slow"]
        if total == 0:
            print("No requests succeeded")
            sys.exit(1)

        fast_ratio = results["fast"] / total
        slow_ratio = results["slow"] / total

        print("\nResults:")
        print(f"  Fast backend: {results['fast']} ({fast_ratio:.1%})")
        print(f"  Slow backend: {results['slow']} ({slow_ratio:.1%})")

        if slow_ratio > 0.15:
            print("FAILED: Slow backend used too frequently! EWMA may not be working.")
            sys.exit(1)
        else:
            print("PASSED: Load balancer avoids slow backend as expected by EWMA.")

    finally:
        deregister_from_consul("test-fast-1")
        deregister_from_consul("test-slow-1")
        print("Cleaned up Consul services")


if __name__ == "__main__":
    main()
