package com.zura.ar_rules.repository;

import com.zura.ar_rules.model.ValidationsResult;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ValidationsResultRepository extends MongoRepository<ValidationsResult,String> {
  public  ValidationsResult findByCentralizedJsonId(String jsonId);
}
