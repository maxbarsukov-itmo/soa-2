import pytest
import requests
from models import ErrorResponse, HairColor, EyeColor, Country, PersonInput

DEMOGRAPHY_URL = pytest.DEMOGRAPHY_URL
PEOPLE_URL = pytest.PEOPLE_URL

def assert_error(response, status_code: int):
    assert response.status_code == status_code
    err = ErrorResponse.model_validate(response.json())
    assert err.code == status_code

@pytest.fixture(scope="module")
def demography_test_data():
    requests.delete(f"{PEOPLE_URL}/people/nationality/NORTH_KOREA", verify=False)

    people = []
    for data in [
        {"name": "Demography_Blue_Eyes", "eyeColor": "BLUE", "hairColor": "BROWN"},
        {"name": "Demography_Red_Eyes", "eyeColor": "RED", "hairColor": "GREEN"},
        {"name": "Demography_No_Hair", "eyeColor": "YELLOW", "hairColor": None},
    ]:
        payload = {
            "name": data["name"],
            "coordinates": {"x": 0, "y": 0},
            "eyeColor": data["eyeColor"],
            "hairColor": data["hairColor"],
            "nationality": "NORTH_KOREA",
            "location": {"x": 0, "y": 0, "z": 0}
        }
        resp = requests.post(f"{PEOPLE_URL}/people", json=payload, verify=False)
        assert resp.status_code == 201
        people.append(resp.json())

    yield people

    requests.delete(f"{PEOPLE_URL}/people/nationality/NORTH_KOREA", verify=False)

def test_hair_color_percentage_valid(demography_test_data):
    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/hair-color/BROWN/percentage", verify=False)
    assert resp.status_code == 200
    percentage = resp.json()
    assert isinstance(percentage, float)
    assert 0.0 <= percentage <= 100.0
    assert 33.0 <= percentage <= 34.0

def test_eye_color_count_valid(demography_test_data):
    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/eye-color/BLUE", verify=False)
    assert resp.status_code == 200
    assert resp.json() == 1

def test_hair_color_percentage_zero():
    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/hair-color/ORANGE/percentage", verify=False)
    assert resp.status_code == 200
    assert resp.json() == 0.0

def test_hair_color_percentage_invalid_enum():
    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/hair-color/PURPLE/percentage", verify=False)
    assert_error(resp, 400)

def test_eye_color_count_zero():
    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/eye-color/ORANGE", verify=False)
    assert resp.status_code == 200
    assert resp.json() == 0.0

def test_eye_color_count_invalid_enum():
    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/eye-color/PURPLE", verify=False)
    assert_error(resp, 400)

def test_demography_consistency():
    resp = requests.get(f"{PEOPLE_URL}/people?pageSize=1000", verify=False)
    people = [p for p in resp.json()["people"] if p.get("nationality") == "NORTH_KOREA"]

    blue_count = sum(1 for p in people if p["eyeColor"] == "BLUE")
    brown_count = sum(1 for p in people if p.get("hairColor") == "BROWN")
    total = len(people)

    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/eye-color/BLUE", verify=False)
    assert resp.status_code == 200
    assert resp.json() == blue_count

    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/hair-color/BROWN/percentage", verify=False)
    assert resp.status_code == 200
    expected_pct = (brown_count / total) * 100
    actual_pct = resp.json()
    assert abs(actual_pct - expected_pct) < 0.01

def test_demography_404_when_collection_empty():
    for nat in ["CHINA", "INDIA", "ITALY", "NORTH_KOREA"]:
        requests.delete(f"{PEOPLE_URL}/people/nationality/{nat}", verify=False)

    resp = requests.get(f"{PEOPLE_URL}/people?pageSize=1000", verify=False)
    assert resp.status_code == 200
    assert len(resp.json()["people"]) == 0

    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/eye-color/BLUE", verify=False)
    assert_error(resp, 404)

    resp = requests.get(f"{DEMOGRAPHY_URL}/demography/hair-color/BROWN/percentage", verify=False)
    assert_error(resp, 404)
