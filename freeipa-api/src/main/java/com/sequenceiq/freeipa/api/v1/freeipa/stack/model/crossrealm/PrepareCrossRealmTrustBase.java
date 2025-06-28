package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrepareCrossRealmTrustBase {
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentCrn;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @Override
    public String toString() {
        return "PrepareCrossRealmTrustBase{" +
                "environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}
