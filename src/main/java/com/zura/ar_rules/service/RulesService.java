package com.zura.ar_rules.service;

import com.zura.ar_rules.model.Rules;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface RulesService {
  public   ResponseEntity<?> createOrUpdateRule(List<Rules> rule);

 public  ResponseEntity<?> getAllRules();

 public  ResponseEntity<?> getValidationsResultOfTimesheet(String jsonId);

   public  ResponseEntity<?> runValidations(String id);
}
