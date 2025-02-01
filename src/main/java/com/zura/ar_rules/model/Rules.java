package com.zura.ar_rules.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Collation;

@Data
@Document(collection="Validations")
public class Rules {

    @Id
    private String id;
    private String rule;
    private String errorMessage;
    private boolean enabled;
    private String query;
    private boolean displayTimesheet;
    private String customQuery;
    private String ruleAcronym;
    private boolean optionalFlag;
}
