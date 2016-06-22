package com.sequenceiq.cloudbreak.api.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsTestResult")
public class RdsTestResult implements JsonEntity {

    @ApiModelProperty(required = true)
    private String connectionResult;

    public RdsTestResult() {

    }

    public RdsTestResult(String connectionResult) {
        this.connectionResult = connectionResult;
    }

    public String getConnectionResult() {
        return connectionResult;
    }

    public void setConnectionResult(String connectionResult) {
        this.connectionResult = connectionResult;
    }
}
