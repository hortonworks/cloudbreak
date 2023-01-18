package com.sequenceiq.distrox.api.v1.distrox.model;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretTypes;
import com.sequenceiq.cloudbreak.rotation.request.BaseSecretRotationRequest;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistroXSecretRotationRequest extends BaseSecretRotationRequest {

    @TenantAwareParam
    @ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
    private String crn;

    @ValidSecretTypes(allowedTypes = { CloudbreakSecretType.class })
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
