package com.sequenceiq.sdx.api.model;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.annotation.OnlyPublicSecretTypes;
import com.sequenceiq.cloudbreak.rotation.annotation.OnlySingleSecretTypes;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxSecretRotationRequest {

    @ValidCrn(resource = CrnResourceDescriptor.DATALAKE)
    @ApiModelProperty(ModelDescriptions.DATA_LAKE_CRN)
    private String crn;

    @OnlyPublicSecretTypes
    @OnlySingleSecretTypes
    @NotEmpty
    @ApiModelProperty("Secrets to be rotated")
    private List<String> secrets;

    @ApiModelProperty("Execution type if needed")
    private RotationFlowExecutionType executionType;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public List<String> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<String> secrets) {
        this.secrets = secrets;
    }

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    public void setExecutionType(RotationFlowExecutionType executionType) {
        this.executionType = executionType;
    }
}
