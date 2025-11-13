package ru.ifmo.soa.peopleservice.ejb.remote;

import jakarta.ejb.Remote;
import ru.ifmo.soa.peopleservice.entities.Location;
import ru.ifmo.soa.peopleservice.util.Result;

@Remote
public interface DeletePersonByLocationRemote {
  Result<Void> deletePersonByLocation(Location location);
}
