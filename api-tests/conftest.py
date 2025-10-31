import pytest

# FIXME https
PEOPLE_URL = "http://localhost:51313/api/v1"
DEMOGRAPHY_URL = "http://localhost:51312/api/v1"

def pytest_configure(config):
    pytest.PEOPLE_URL = PEOPLE_URL
    pytest.DEMOGRAPHY_URL = DEMOGRAPHY_URL
