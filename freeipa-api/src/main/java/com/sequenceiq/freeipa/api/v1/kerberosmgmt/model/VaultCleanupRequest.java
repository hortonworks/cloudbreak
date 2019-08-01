package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("VaultCleanupV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VaultCleanupRequest {

    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    @NotNull
    private String environmentCrn;

    @ApiModelProperty(value = ModelDescriptions.CLUSTER_CRN)
    private String clusterCrn;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public void setClusterCrn(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }
}