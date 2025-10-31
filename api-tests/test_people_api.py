import pytest
import requests
import uuid
import json
from models import (
    PersonInput, Person, PeopleResponse, FilterCriteria,
    EyeColor, HairColor, Country, Coordinates, Location, ErrorResponse
)
from callback_server import start_callback_server, get_callback_result

BASE_URL = pytest.BASE_URL

def assert_error(response, status_code: int):
    assert response.status_code == status_code
    err = ErrorResponse.model_validate(response.json())
    assert err.code == status_code

created_ids = []

def test_get_people_default():
    resp = requests.get(f"{BASE_URL}/people", verify=False)
    assert resp.status_code == 200
    data = PeopleResponse.model_validate(resp.json())
    assert data.page == 0
    assert data.pageSize == 10
    assert len(data.people) == 0

def test_pagination():
    global created_ids
    created_ids = []
    for i in range(15):
        payload = {
            "name": f"Person{i}",
            "coordinates": {"x": i, "y": i},
            "eyeColor": "BLUE",
            "location": {"x": i, "y": i, "z": i}
        }
        p = create_person(payload)
        created_ids.append(p.id)

    resp = requests.get(f"{BASE_URL}/people?page=1&pageSize=10", verify=False)
    assert resp.status_code == 200
    data = PeopleResponse.model_validate(resp.json())
    assert len(data.people) == 5
    assert data.totalCount == 15
    assert data.totalPages == 2

def test_invalid_page_size():
    resp = requests.get(f"{BASE_URL}/people?pageSize=-1", verify=False)
    assert_error(resp, 422)
    resp = requests.post(f"{BASE_URL}/people/search?pageSize=-1", json={}, verify=False)
    assert_error(resp, 422)

def test_invalid_page():
    resp = requests.get(f"{BASE_URL}/people?page=-1", verify=False)
    assert_error(resp, 422)
    resp = requests.post(f"{BASE_URL}/people/search?page=-1", json={}, verify=False)
    assert_error(resp, 422)

def test_sorting_all_fields():
    p1 = create_person({
        "name": "Alpha",
        "coordinates": {"x": 1, "y": 1},
        "eyeColor": "BLUE",
        "hairColor": "BROWN",
        "nationality": "CHINA",
        "height": 1.7,
        "location": {"x": 1, "y": 1, "z": 1, "name": "A"}
    })
    p2 = create_person({
        "name": "Beta",
        "coordinates": {"x": 2, "y": 2},
        "eyeColor": "RED",
        "hairColor": "GREEN",
        "nationality": "ITALY",
        "height": 1.9,
        "location": {"x": 2, "y": 2, "z": 2, "name": "B"}
    })

    fields_and_values = {
        "id": (p1.id, p2.id),
        "name": ("Alpha", "Beta"),
        "creationDate": (p1.creationDate, p2.creationDate),
        "coordinates.x": (1, 2),
        "coordinates.y": (1, 2),
        "height": (1.7, 1.9),
        "eyeColor": ("RED", "BLUE"),
        "hairColor": ("GREEN", "BROWN"),
        "nationality": ("CHINA", "ITALY"),
        "location.x": (1, 2),
        "location.y": (1, 2),
        "location.z": (1, 2),
        "location.name": ("A", "B")
    }

    for field, (val1, val2) in fields_and_values.items():
        resp = requests.get(f"{BASE_URL}/people?sortBy={field}&sortOrder=asc&pageSize=1000", verify=False)
        assert resp.status_code == 200, f"Failed sorting by {field}"
        data = PeopleResponse.model_validate(resp.json())
        names = [p.name for p in data.people if p.name in ("Alpha", "Beta")]
        assert names[0] == "Alpha", f"Field {field}: expected Alpha first, got {names}"

        resp = requests.get(f"{BASE_URL}/people?sortBy={field}&sortOrder=desc&pageSize=1000", verify=False)
        assert resp.status_code == 200, f"Failed sorting by {field} desc"
        data = PeopleResponse.model_validate(resp.json())
        names = [p.name for p in data.people if p.name in ("Alpha", "Beta")]
        assert names[0] == "Beta", f"Field {field}: expected Beta first in desc, got {names}"

def test_sorting():
    p1 = create_person({"name": "Alice_Z", "coordinates": {"x": 999, "y": 999}, "eyeColor": "RED", "location": {"x": 0, "y": 0, "z": 0}})
    p2 = create_person({"name": "Bob_Z", "coordinates": {"x": 998, "y": 998}, "eyeColor": "RED", "location": {"x": 0, "y": 0, "z": 0}})

    resp = requests.get(f"{BASE_URL}/people?sortBy=name&sortOrder=asc&pageSize=1000", verify=False)
    data = PeopleResponse.model_validate(resp.json())
    names = [p.name for p in data.people]
    assert "Alice_Z" in names
    assert "Bob_Z" in names
    assert names.index("Alice_Z") < names.index("Bob_Z")

    resp = requests.get(f"{BASE_URL}/people?sortBy=name&sortOrder=desc&pageSize=1000", verify=False)
    data = PeopleResponse.model_validate(resp.json())
    names = [p.name for p in data.people]
    assert names.index("Bob_Z") < names.index("Alice_Z")

def test_invalid_sort_field():
    resp = requests.get(f"{BASE_URL}/people?sortBy=invalidField", verify=False)
    assert_error(resp, 400)

def test_post_person():
    payload = {
        "name": f"Max_{uuid.uuid4().hex[:6]}",
        "coordinates": {"x": 10, "y": 20},
        "eyeColor": "BLUE",
        "location": {"x": 1, "y": 2, "z": 3, "name": "Home"}
    }
    resp = requests.post(f"{BASE_URL}/people", json=payload, verify=False)
    assert resp.status_code == 201
    person = Person.model_validate(resp.json())
    assert person.name == payload["name"]
    assert person.coordinates.x == 10
    assert person.eyeColor == EyeColor.BLUE
    assert person.nationality is None

def test_post_person_conflict():
    name = f"Unique_{uuid.uuid4().hex[:6]}"
    payload = {
        "name": name,
        "coordinates": {"x": 777, "y": 777},
        "eyeColor": "RED",
        "location": {"x": 0, "y": 0, "z": 0}
    }
    create_person(payload)
    resp = requests.post(f"{BASE_URL}/people", json=payload, verify=False)
    assert_error(resp, 409)

def test_invalid_enum_value():
    payload = {
        "name": f"BadEnum_{uuid.uuid4().hex[:6]}",
        "coordinates": {"x": 0, "y": 0},
        "eyeColor": "PURPLE",
        "location": {"x": 0, "y": 0, "z": 0}
    }
    resp = requests.post(f"{BASE_URL}/people", json=payload, verify=False)
    assert_error(resp, 422)

def test_post_person_wrong_content_type():
    payload = {"name": "Test", "coordinates": {"x":0,"y":0}, "eyeColor": "BLUE", "location": {"x":0,"y":0,"z":0}}
    resp = requests.post(
        f"{BASE_URL}/people",
        data=json.dumps(payload),
        headers={"Content-Type": "text/plain"},
        verify=False
    )
    assert_error(resp, 415)

def test_post_person_too_large():
    huge_name = "A" * (20 * 1024 * 1024) # 20 MiB
    payload = {
        "name": huge_name,
        "coordinates": {"x": 0, "y": 0},
        "eyeColor": "BLUE",
        "location": {"x": 0, "y": 0, "z": 0}
    }
    resp = requests.post(f"{BASE_URL}/people", json=payload, verify=False, timeout=10)
    assert_error(resp, 413)

def test_post_person_insufficient_storage():
    pytest.skip("507 requires server-side storage simulation")

def test_get_person_by_id():
    p = create_person({"name": f"ByID_{uuid.uuid4().hex[:6]}", "coordinates": {"x": 0, "y": 0}, "eyeColor": "BLUE", "location": {"x": 0, "y": 0, "z": 0}})
    resp = requests.get(f"{BASE_URL}/people/{p.id}", verify=False)
    assert resp.status_code == 200
    fetched = Person.model_validate(resp.json())
    assert fetched.id == p.id
    assert fetched.name == p.name

def test_get_nonexistent_person():
    resp = requests.get(f"{BASE_URL}/people/999999", verify=False)
    assert_error(resp, 404)

def test_patch_person():
    p = create_person({"name": f"Old_{uuid.uuid4().hex[:6]}", "coordinates": {"x": 0, "y": 0}, "eyeColor": "BLUE", "location": {"x": 0, "y": 0, "z": 0}})
    resp = requests.patch(f"{BASE_URL}/people/{p.id}", json={"name": "NewName"}, verify=False)
    assert resp.status_code == 200
    updated = Person.model_validate(resp.json())
    assert updated.name == "NewName"
    assert updated.id == p.id
    assert updated.eyeColor == p.eyeColor

def test_delete_person():
    p = create_person({"name": f"Del_{uuid.uuid4().hex[:6]}", "coordinates": {"x": 0, "y": 0}, "eyeColor": "BLUE", "location": {"x": 0, "y": 0, "z": 0}})
    resp = requests.delete(f"{BASE_URL}/people/{p.id}", verify=False)
    assert resp.status_code == 204
    resp = requests.get(f"{BASE_URL}/people/{p.id}", verify=False)
    assert_error(resp, 404)

def test_delete_by_nationality():
    cn = create_person({"name": "CN1", "coordinates": {"x": 0, "y": 0}, "eyeColor": "BLUE", "nationality": "CHINA", "location": {"x": 0, "y": 0, "z": 0}})
    it = create_person({"name": "IT1", "coordinates": {"x": 0, "y": 0}, "eyeColor": "BLUE", "nationality": "ITALY", "location": {"x": 0, "y": 0, "z": 0}})

    resp = requests.delete(f"{BASE_URL}/people/nationality/CHINA", verify=False)
    assert resp.status_code == 204

    resp = requests.get(f"{BASE_URL}/people?pageSize=1000", verify=False)
    people = resp.json()["people"]
    italian = [p for p in people if p.get("nationality") == "ITALY"]
    chinese = [p for p in people if p.get("nationality") == "CHINA"]
    assert len(italian) >= 1
    assert len(chinese) == 0

def test_delete_by_location():
    loc_person = create_person({
        "name": "ToDeleteByLoc",
        "coordinates": {"x": 0, "y": 0},
        "eyeColor": "BLUE",
        "location": {"x": 999, "y": 888, "z": 777, "name": "Target"}
    })
    location_payload = {"x": 999, "y": 888, "z": 777, "name": "Target"}
    resp = requests.delete(f"{BASE_URL}/people/location", json=location_payload, verify=False)
    assert resp.status_code == 204

    resp = requests.get(f"{BASE_URL}/people?pageSize=1000", verify=False)
    names = [p["name"] for p in resp.json()["people"]]
    assert "ToDeleteByLoc" not in names

def test_location_y_int64():
    big_y = 1125899906842624 # 2^50
    p = create_person({
        "name": "BigY",
        "coordinates": {"x": 0, "y": 0},
        "eyeColor": "BLUE",
        "location": {"x": 0, "y": big_y, "z": 0}
    })
    assert p.location.y == big_y

def test_search_people():
    p = create_person({
        "name": "SearchMe_Sync",
        "coordinates": {"x": 100, "y": 200},
        "eyeColor": "RED",
        "height": 1.8,
        "location": {"x": 10, "y": 20, "z": 30, "name": "City"}
    })
    criteria = {
        "filters": [
            {"field": "name", "operator": "eq", "value": "SearchMe_Sync"},
            {"field": "height", "operator": "gt", "value": "1.7"}
        ]
    }
    resp = requests.post(f"{BASE_URL}/people/search", json=criteria, verify=False)
    assert resp.status_code == 200
    data = PeopleResponse.model_validate(resp.json())
    assert len(data.people) == 1
    assert data.people[0].name == "SearchMe_Sync"

def test_search_people_with_callback():
    server, callback_url = start_callback_server()

    person = create_person({
        "name": "CallbackTest_Final",
        "coordinates": {"x": 10, "y": 20},
        "eyeColor": "BLUE",
        "height": 1.85,
        "location": {"x": 100, "y": 200, "z": 300, "name": "CallbackCity"}
    })

    criteria = {
        "filters": [
            {"field": "name", "operator": "eq", "value": "CallbackTest_Final"}
        ]
    }

    resp = requests.post(
        f"{BASE_URL}/people/search",
        json=criteria,
        headers={"X-Callback-URL": callback_url},
        verify=False
    )
    assert resp.status_code == 202
    task_info = resp.json()
    task_id = task_info["taskId"]

    result = get_callback_result("last", timeout=15)
    assert result is not None
    assert "data" in result
    people = result["data"]["people"]
    assert len(people) == 1
    assert people[0]["name"] == "CallbackTest_Final"

    result = get_callback_result(task_id, timeout=15)
    assert result is not None
    assert "data" in result
    people = result["data"]["people"]
    assert len(people) == 1
    assert people[0]["name"] == "CallbackTest_Final"


def test_location_greater():
    create_person({"name": "Low_Loc", "coordinates": {"x": 0, "y": 0}, "eyeColor": "BLUE", "location": {"x": 1, "y": 1, "z": 1}})
    create_person({"name": "High_Loc", "coordinates": {"x": 0, "y": 0}, "eyeColor": "BLUE", "location": {"x": 10, "y": 10, "z": 10}})

    resp = requests.get(f"{BASE_URL}/people/location/greater?x=5&y=5&z=5", verify=False)
    assert resp.status_code == 200
    data = PeopleResponse.model_validate(resp.json())
    names = [p.name for p in data.people]
    assert "High_Loc" in names
    assert "Low_Loc" not in names

def test_invalid_id():
    resp = requests.get(f"{BASE_URL}/people/-1", verify=False)
    assert_error(resp, 400)

def test_invalid_nationality():
    resp = requests.delete(f"{BASE_URL}/people/nationality/INVALID", verify=False)
    assert_error(resp, 400)

def create_person(data: dict) -> Person:
    resp = requests.post(f"{BASE_URL}/people", json=data, verify=False)
    assert resp.status_code == 201, f"Failed to create person: {resp.status_code} {resp.text}"
    return Person.model_validate(resp.json())
