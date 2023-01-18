package com.sequenceiq.distrox.api.v1.distrox.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistroXSecretTypeResponse {

    @NotNull
    private String secretType;

    private String description;

    public DistroXSecretTypeResponse() {
    }

    public DistroXSecretTypeResponse(String secretType, String description) {
        this.secretType = secretType;
        this.description = description;
    }

    public String getSecretType() {
        return secretType;
    }

    public void setSecretType(String secretType) {
        this.secretType = secretType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "DistroXSecretTypeResponse{" +
                "secretType='" + secretType + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
