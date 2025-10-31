package ru.ifmo.soa.peopleservice.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.ifmo.soa.peopleservice.dto.CoordinatesDto;
import ru.ifmo.soa.peopleservice.dto.LocationDto;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.dto.PersonInputDto;
import ru.ifmo.soa.peopleservice.entities.Coordinates;
import ru.ifmo.soa.peopleservice.entities.Location;
import ru.ifmo.soa.peopleservice.entities.Person;

import java.util.List;

@Mapper(componentModel = "cdi")
@ApplicationScoped
public interface PersonMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "creationDate", ignore = true)
  @Mapping(target = "eyeColor", source = "eyeColor", qualifiedByName = "stringToEnum")
  @Mapping(target = "hairColor", source = "hairColor", qualifiedByName = "stringToEnum")
  @Mapping(target = "nationality", source = "nationality", qualifiedByName = "stringToEnum")
  Person toEntity(PersonInputDto dto);

  @Mapping(target = "eyeColor", source = "eyeColor", qualifiedByName = "enumToString")
  @Mapping(target = "hairColor", source = "hairColor", qualifiedByName = "enumToString")
  @Mapping(target = "nationality", source = "nationality", qualifiedByName = "enumToString")
  PersonDto toDto(Person entity);

  List<PersonDto> toDtoList(List<Person> entities);

  @Named("stringToEnum")
  default ru.ifmo.soa.peopleservice.entities.EyeColor stringToEyeColor(String value) {
    return value != null ? ru.ifmo.soa.peopleservice.entities.EyeColor.valueOf(value) : null;
  }

  @Named("stringToEnum")
  default ru.ifmo.soa.peopleservice.entities.HairColor stringToHairColor(String value) {
    return value != null ? ru.ifmo.soa.peopleservice.entities.HairColor.valueOf(value) : null;
  }

  @Named("stringToEnum")
  default ru.ifmo.soa.peopleservice.entities.Country stringToCountry(String value) {
    return value != null ? ru.ifmo.soa.peopleservice.entities.Country.valueOf(value) : null;
  }

  @Named("enumToString")
  default String enumToEyeColor(ru.ifmo.soa.peopleservice.entities.EyeColor value) {
    return value != null ? value.name() : null;
  }

  @Named("enumToString")
  default String enumToHairColor(ru.ifmo.soa.peopleservice.entities.HairColor value) {
    return value != null ? value.name() : null;
  }

  @Named("enumToString")
  default String enumToCountry(ru.ifmo.soa.peopleservice.entities.Country value) {
    return value != null ? value.name() : null;
  }
}
