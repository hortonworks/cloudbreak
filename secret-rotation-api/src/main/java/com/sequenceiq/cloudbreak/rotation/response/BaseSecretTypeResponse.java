package com.sequenceiq.cloudbreak.rotation.response;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

public class BaseSecretTypeResponse {

    @NotNull
    @Schema(description = "Secret type", requiredMode = Schema.RequiredMode.REQUIRED)
    private String secretType;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Last updated")
    private Long lastUpdated;

    public BaseSecretTypeResponse() {
    }

    public BaseSecretTypeResponse(String secretType, String displayName, String description, Long lastUpdated) {
        this.secretType = secretType;
        this.displayName = displayName;
        this.description = description;
        this.lastUpdated = lastUpdated;
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

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
