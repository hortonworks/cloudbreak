package com.sequenceiq.cloudbreak.api.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AmbariDatabaseTestResult")
public class AmbariDatabaseTestResult implements JsonEntity {

    @ApiModelProperty(required = true)
    private String error;

    public AmbariDatabaseTestResult() {
    }

    public AmbariDatabaseTestResult(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
