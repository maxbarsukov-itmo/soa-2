from datetime import datetime
from enum import Enum
from typing import List, Optional, Any
from pydantic import BaseModel, Field

class EyeColor(str, Enum):
    RED = "RED"
    BLUE = "BLUE"
    YELLOW = "YELLOW"
    ORANGE = "ORANGE"

class HairColor(str, Enum):
    GREEN = "GREEN"
    RED = "RED"
    YELLOW = "YELLOW"
    ORANGE = "ORANGE"
    BROWN = "BROWN"

class Country(str, Enum):
    CHINA = "CHINA"
    INDIA = "INDIA"
    ITALY = "ITALY"
    NORTH_KOREA = "NORTH_KOREA"

class Coordinates(BaseModel):
    x: int
    y: int

class Location(BaseModel):
    x: int
    y: int
    z: int
    name: Optional[str] = None

class Person(BaseModel):
    id: int
    name: str
    coordinates: Coordinates
    creationDate: datetime
    height: Optional[float] = None
    eyeColor: EyeColor
    hairColor: Optional[HairColor] = None
    nationality: Optional[Country] = None
    location: Location

class PersonInput(BaseModel):
    name: str
    coordinates: Coordinates
    height: Optional[float] = None
    eyeColor: EyeColor
    hairColor: Optional[HairColor] = None
    nationality: Optional[Country] = None
    location: Location

class PeopleResponse(BaseModel):
    people: List[Person]
    page: Optional[int] = None
    pageSize: Optional[int] = None
    totalPages: Optional[int] = None
    totalCount: Optional[int] = None

class FilterCondition(BaseModel):
    field: str
    operator: str
    value: str

class FilterCriteria(BaseModel):
    filters: List[FilterCondition]

class ErrorResponse(BaseModel):
    code: int
    message: str
    time: datetime
