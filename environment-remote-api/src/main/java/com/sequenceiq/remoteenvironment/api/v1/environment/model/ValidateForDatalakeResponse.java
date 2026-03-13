package com.sequenceiq.remoteenvironment.api.v1.environment.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.remoteenvironment.api.v1.environment.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateForDatalakeResponse {

    @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.CLASSIC_CLUSTER})
    @Schema(description = ModelDescriptions.CRN)
    @ResourceCrn
    private String crn;

    private boolean valid;

    private List<ValidateForDatalakeValidationResponse> validations;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ValidateForDatalakeValidationResponse> getValidations() {
        return validations;
    }

    public void setValidations(List<ValidateForDatalakeValidationResponse> validations) {
        this.validations = validations;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ValidateForDatalakeResponse that)) {
            return false;
        }
        return valid == that.valid && Objects.equals(crn, that.crn) && Objects.equals(validations, that.validations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn, valid, validations);
    }

    @Override
    public String toString() {
        return "ValidateForDatalakeResponse{" +
                "crn='" + crn + '\'' +
                ", valid=" + valid +
                ", validations=" + validations +
                '}';
    }
}
