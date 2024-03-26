package com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration;

import java.util.Objects;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrivateControlPlaneDeRegistrationV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateControlPlaneDeRegistrationResponse {

    @NotEmpty
    @Schema
    private String crn;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrivateControlPlaneDeRegistrationResponse that = (PrivateControlPlaneDeRegistrationResponse) o;
        return Objects.equals(crn, that.crn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn);
    }

    @Override
    public String toString() {
        return "PrivateControlPlaneDeRegistrationResponse{" +
                "crn='" + crn + '\'' +
                '}';
    }
}
