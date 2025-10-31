from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse
import json
import threading
import time

received_callbacks = {}

class CallbackHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path == "/webhook/search-results":
            content_len = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_len)
            try:
                data = json.loads(body.decode("utf-8"))
                task_id = data.get("correlationId") or data.get("taskId", "unknown")
                print("Received callback:", data)
                received_callbacks["last"] = data
                received_callbacks[task_id] = data
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps({"status": "success"}).encode("utf-8"))
            except Exception as e:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b"Invalid JSON")
        else:
            self.send_response(404)
            self.end_headers()

def start_callback_server(port=8089):
    server = HTTPServer(("localhost", port), CallbackHandler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    return server, f"http://localhost:{port}/webhook/search-results"

def get_callback_result(task_id: str, timeout: int = 10) -> dict | None:
    start = time.time()
    while time.time() - start < timeout:
        if task_id in received_callbacks:
            return received_callbacks.pop(task_id)
        time.sleep(0.2)
    return None
