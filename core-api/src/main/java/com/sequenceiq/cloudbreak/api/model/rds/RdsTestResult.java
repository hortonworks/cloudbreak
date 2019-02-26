package com.sequenceiq.cloudbreak.api.model.rds;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfig;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsTestResult")
public class RdsTestResult implements JsonEntity {

    @ApiModelProperty(value = RDSConfig.RDS_CONNECTION_TEST_RESULT, required = true)
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
