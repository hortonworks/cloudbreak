package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.DatabaseUpgradeStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseUpgradeStatus extends DatabaseUpgradeStatus {

    @Schema(description = ModelDescriptions.DATALAKE_CRN)
    private String datalakeCrn;

    public SdxDatabaseUpgradeStatus() {
    }

    private SdxDatabaseUpgradeStatus(String datalakeCrn, String upgradeStatus,
            String targetMajorVersion, String currentMajorVersion) {
        super(upgradeStatus, targetMajorVersion, currentMajorVersion);
        this.datalakeCrn = datalakeCrn;
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

    @Override
    public String toString() {
        return "SdxDatabaseUpgradeStatus{"
                + "datalakeCrn='" + datalakeCrn + '\''
                + ", upgradeStatus='" + getUpgradeStatus() + '\''
                + ", targetMajorVersion='" + getTargetMajorVersion() + '\''
                + ", currentMajorVersion='" + getCurrentMajorVersion() + '\''
                + ", eolDate='" + getEolDate() + '\''
                + '}';
    }
}
