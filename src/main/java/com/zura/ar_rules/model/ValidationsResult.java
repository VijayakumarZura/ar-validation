package com.zura.ar_rules.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "ValidationsResult")
public class ValidationsResult {

    @Id
    private  String id;

    private  String centralizedJsonId;

    private Object result;

}
