package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("VaultCleanupV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VaultCleanupRequest {

    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    @NotNull
    @ResourceObjectField(action = AuthorizationResourceAction.EDIT_ENVIRONMENT, variableType = AuthorizationVariableType.CRN)
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

    @Override
    public String toString() {
        return "VaultCleanupRequest{"
                + "environmentCrn='" + environmentCrn + '\''
                + ", clusterCrn='" + clusterCrn + '\''
                + '}';
    }
}