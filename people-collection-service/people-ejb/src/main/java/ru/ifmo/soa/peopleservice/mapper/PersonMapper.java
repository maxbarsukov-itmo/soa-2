package ru.ifmo.soa.peopleservice.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.ifmo.soa.peopleservice.dto.*;
import ru.ifmo.soa.peopleservice.entities.*;

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
  default EyeColor stringToEyeColor(String value) {
    return value != null ? EyeColor.valueOf(value) : null;
  }

  @Named("stringToEnum")
  default HairColor stringToHairColor(String value) {
    return value != null ? HairColor.valueOf(value) : null;
  }

  @Named("stringToEnum")
  default Country stringToCountry(String value) {
    return value != null ? Country.valueOf(value) : null;
  }

  @Named("enumToString")
  default String enumToEyeColor(EyeColor value) {
    return value != null ? value.name() : null;
  }

  @Named("enumToString")
  default String enumToHairColor(HairColor value) {
    return value != null ? value.name() : null;
  }

  @Named("enumToString")
  default String enumToCountry(Country value) {
    return value != null ? value.name() : null;
  }
}
