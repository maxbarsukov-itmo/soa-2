import pytest

# FIXME
BASE_URL = "http://127.0.0.1:8080/people-collection-service/api/v1"

def pytest_configure(config):
    pytest.BASE_URL = BASE_URL
