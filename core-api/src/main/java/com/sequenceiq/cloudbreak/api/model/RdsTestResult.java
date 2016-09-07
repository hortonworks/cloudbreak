package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsTestResult")
public class RdsTestResult implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.RDS_CONNECTION_RESULT, required = true)
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
