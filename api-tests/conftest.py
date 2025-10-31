import pytest

# FIXME https
PEOPLE_URL = "http://localhost:51313/api/v1"

def pytest_configure(config):
    pytest.PEOPLE_URL = PEOPLE_URL
