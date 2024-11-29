package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeipaSecretTypeResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeipaSecretTypeResponse {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String secretType;

    private String displayName;

    private String description;

    public FreeipaSecretTypeResponse() {
    }

    public FreeipaSecretTypeResponse(String secretType, String displayName, String description) {
        this.secretType = secretType;
        this.displayName = displayName;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return "FreeipaSecretTypeResponse{" +
                "secretType='" + secretType + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
