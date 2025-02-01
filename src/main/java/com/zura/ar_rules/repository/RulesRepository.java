package com.zura.ar_rules.repository;

import com.zura.ar_rules.model.Rules;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RulesRepository extends MongoRepository<Rules,String> {
  public  List<Rules> findByEnabled(boolean b);
}
