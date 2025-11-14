import pytest

BASE_GATEWAY_URL = "http://localhost:8765/api/v1"
PEOPLE_URL = BASE_GATEWAY_URL
DEMOGRAPHY_URL = BASE_GATEWAY_URL

def pytest_configure(config):
    pytest.PEOPLE_URL = PEOPLE_URL
    pytest.DEMOGRAPHY_URL = DEMOGRAPHY_URL
