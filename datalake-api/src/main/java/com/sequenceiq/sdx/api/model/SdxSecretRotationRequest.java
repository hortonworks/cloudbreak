package com.sequenceiq.sdx.api.model;

import java.util.Set;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
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

    @NotEmpty
    @ApiModelProperty("Secrets to be rotated")
    private Set<String> secrets;

    @ApiModelProperty("Execution type if needed")
    private RotationFlowExecutionType executionType;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public Set<String> getSecrets() {
        return secrets;
    }

    public void setSecrets(Set<String> secrets) {
        this.secrets = secrets;
    }

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    public void setExecutionType(RotationFlowExecutionType executionType) {
        this.executionType = executionType;
    }
}
