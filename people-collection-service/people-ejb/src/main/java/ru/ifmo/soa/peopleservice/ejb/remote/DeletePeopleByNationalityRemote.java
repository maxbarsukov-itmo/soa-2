package ru.ifmo.soa.peopleservice.ejb.remote;

import jakarta.ejb.Remote;
import ru.ifmo.soa.peopleservice.util.Result;

@Remote
public interface DeletePeopleByNationalityRemote {
  Result<Void> deletePeopleByNationality(String nationality);
}
