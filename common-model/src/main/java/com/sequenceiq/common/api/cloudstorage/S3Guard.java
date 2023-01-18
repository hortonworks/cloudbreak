package com.sequenceiq.common.api.cloudstorage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3Guard {

    private String dynamoTableName;

    public String getDynamoTableName() {
        return dynamoTableName;
    }

    public void setDynamoTableName(String dynamoTableName) {
        this.dynamoTableName = dynamoTableName;
    }
}