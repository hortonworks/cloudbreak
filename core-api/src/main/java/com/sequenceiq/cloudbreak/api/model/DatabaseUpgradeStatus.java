package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DatabaseUpgradeStatus {

    @Schema(description = "The RDS upgrade status.")
    private String upgradeStatus;

    @Schema(description = "The target PostgreSQL major version (e.g. '14'); present only when status is UPGRADE_REQUIRED.")
    private String targetMajorVersion;

    @Schema(description = "The current PostgreSQL major version (e.g. '11'); present when the DB server was reachable.")
    private String currentMajorVersion;

    protected DatabaseUpgradeStatus() {
    }

    protected DatabaseUpgradeStatus(String upgradeStatus, String targetMajorVersion, String currentMajorVersion) {
        this.upgradeStatus = upgradeStatus;
        this.targetMajorVersion = targetMajorVersion;
        this.currentMajorVersion = currentMajorVersion;
    }

    public String getUpgradeStatus() {
        return upgradeStatus;
    }

    public void setUpgradeStatus(String upgradeStatus) {
        this.upgradeStatus = upgradeStatus;
    }

    public String getTargetMajorVersion() {
        return targetMajorVersion;
    }

    public void setTargetMajorVersion(String targetMajorVersion) {
        this.targetMajorVersion = targetMajorVersion;
    }

    public String getCurrentMajorVersion() {
        return currentMajorVersion;
    }

    public void setCurrentMajorVersion(String currentMajorVersion) {
        this.currentMajorVersion = currentMajorVersion;
    }
}
