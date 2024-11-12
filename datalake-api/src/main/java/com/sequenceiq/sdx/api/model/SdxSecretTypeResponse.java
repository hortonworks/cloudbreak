package com.sequenceiq.sdx.api.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxSecretTypeResponse {

    @NotNull
    @Schema(description = "Secret type")
    private String secretType;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Display name")
    private String displayName;

    public SdxSecretTypeResponse() {
    }

    public SdxSecretTypeResponse(String secretType, String displayName, String description) {
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
        return "SdxSecretTypeResponse{" +
                "secretType='" + secretType + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
