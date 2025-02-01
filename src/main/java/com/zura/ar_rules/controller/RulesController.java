package com.zura.ar_rules.controller;

import com.zura.ar_rules.model.Rules;
import com.zura.ar_rules.service.RulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class RulesController {

    @Autowired
    RulesService rulesService;

    @PostMapping("/createOrUpdateRule")
    public ResponseEntity<?> createOrUpdateRule(@RequestBody List<Rules> rule)
    {
        return  rulesService.createOrUpdateRule(rule);
    }

    @GetMapping("/getAllRules")
    public ResponseEntity<?> getAllRules()
    {
        return  rulesService.getAllRules();
    }

    @GetMapping("/getTimesheetValidationsResult")
    public  ResponseEntity<?> getValidationsResultOfTimesheet(@RequestParam("jsonId") String jsonId)
    {
        return  rulesService.getValidationsResultOfTimesheet(jsonId);
    }

    @GetMapping("/runValidations")
    public ResponseEntity<?> runValidations(@RequestParam("jsonId") String id)
    {
        return  rulesService.runValidations(id);
    }

}
