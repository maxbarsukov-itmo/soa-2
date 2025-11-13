package ru.ifmo.soa.peopleservice.ejb.remote;

import jakarta.ejb.Remote;
import ru.ifmo.soa.peopleservice.util.Result;

@Remote
public interface DeletePersonRemote {
  Result<Void> deletePerson(Long id);
}
