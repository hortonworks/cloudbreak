package com.sequenceiq.distrox.api.v1.distrox.model;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistroXSecretRotationRequest {

    @ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
    private String crn;

    @NotEmpty
    private List<String> secrets;

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
