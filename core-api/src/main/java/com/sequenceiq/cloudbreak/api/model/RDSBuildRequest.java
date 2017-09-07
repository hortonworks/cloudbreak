package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfig;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RDSBuildRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RDSBuildRequest implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = RDSConfig.RDS_REQUEST, required = true)
    private RDSConfigRequest rdsConfigRequest;

    @NotNull
    @ApiModelProperty(value = RDSConfig.RDS_REQUEST_CLUSTER_NAME, required = true)
    private String clusterName;

    public RDSConfigRequest getRdsConfigRequest() {
        return rdsConfigRequest;
    }

    public void setRdsConfigRequest(RDSConfigRequest rdsConfigRequest) {
        this.rdsConfigRequest = rdsConfigRequest;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
}
