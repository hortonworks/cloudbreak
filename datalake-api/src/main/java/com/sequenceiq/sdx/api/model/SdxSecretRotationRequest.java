package com.sequenceiq.sdx.api.model;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretTypes;
import com.sequenceiq.cloudbreak.rotation.request.BaseSecretRotationRequest;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxSecretRotationRequest extends BaseSecretRotationRequest {

    @TenantAwareParam
    @ValidCrn(resource = CrnResourceDescriptor.DATALAKE)
    @Schema(description = ModelDescriptions.DATA_LAKE_CRN)
    private String crn;

    @ValidSecretTypes(allowedTypes = { DatalakeSecretType.class })
    @NotEmpty
    @Schema(description = "Secrets to be rotated")
    private List<String> secrets;

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
}
