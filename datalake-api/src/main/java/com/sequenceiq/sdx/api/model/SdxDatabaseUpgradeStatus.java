package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseUpgradeStatus {

    @Schema(description = ModelDescriptions.DATALAKE_CRN)
    private String datalakeCrn;

    @Schema(description = ModelDescriptions.RDS_UPGRADE_STATUS)
    private String upgradeStatus;

    @Schema(description = ModelDescriptions.TARGET_MAJOR_VERSION_STRING)
    private String targetMajorVersion;

    @Schema(description = ModelDescriptions.CURRENT_MAJOR_VERSION_STRING)
    private String currentMajorVersion;

    public SdxDatabaseUpgradeStatus() {
    }

    private SdxDatabaseUpgradeStatus(String datalakeCrn, String upgradeStatus,
            String targetMajorVersion, String currentMajorVersion) {
        this.datalakeCrn = datalakeCrn;
        this.upgradeStatus = upgradeStatus;
        this.targetMajorVersion = targetMajorVersion;
        this.currentMajorVersion = currentMajorVersion;
    }

    public static SdxDatabaseUpgradeStatus noDatalake(String datalakeCrn) {
        return new SdxDatabaseUpgradeStatus(datalakeCrn, "NO_DATALAKE", null, null);
    }

public static SdxDatabaseUpgradeStatus upgradeNotRequired(String datalakeCrn, String currentVersion) {
        return new SdxDatabaseUpgradeStatus(datalakeCrn, "UPGRADE_NOT_REQUIRED", null, currentVersion);
    }

    public static SdxDatabaseUpgradeStatus upgradeRequired(String datalakeCrn, String targetVersion, String currentVersion) {
        return new SdxDatabaseUpgradeStatus(datalakeCrn, "UPGRADE_REQUIRED", targetVersion, currentVersion);
    }

    public static SdxDatabaseUpgradeStatus unknown(String datalakeCrn) {
        return new SdxDatabaseUpgradeStatus(datalakeCrn, "UNKNOWN", null, null);
    }

    public String getDatalakeCrn() {
        return datalakeCrn;
    }

    public void setDatalakeCrn(String datalakeCrn) {
        this.datalakeCrn = datalakeCrn;
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

    @Override
    public String toString() {
        return "SdxDatabaseUpgradeStatus{" +
                "datalakeCrn='" + datalakeCrn + '\'' +
                ", upgradeStatus='" + upgradeStatus + '\'' +
                ", targetMajorVersion='" + targetMajorVersion + '\'' +
                ", currentMajorVersion='" + currentMajorVersion + '\'' +
                '}';
    }
}
