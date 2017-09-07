package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AmbariDatabaseTestResult implements JsonEntity {

    @ApiModelProperty(value = ClusterModelDescription.AMBARI_DATABASE_ERROR, required = true)
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
