package com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.DatabaseUpgradeStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistroXDatabaseUpgradeStatus extends DatabaseUpgradeStatus {

    @Schema(description = "The CRN of the Datahub.")
    private String datahubCrn;

    public DistroXDatabaseUpgradeStatus() {
    }

    private DistroXDatabaseUpgradeStatus(String datahubCrn, String upgradeStatus,
            String targetMajorVersion, String currentMajorVersion) {
        super(upgradeStatus, targetMajorVersion, currentMajorVersion);
        this.datahubCrn = datahubCrn;
    }

    public static DistroXDatabaseUpgradeStatus noDatahub(String datahubCrn) {
        return new DistroXDatabaseUpgradeStatus(datahubCrn, "NO_DATAHUB", null, null);
    }

    public static DistroXDatabaseUpgradeStatus upgradeNotRequired(String datahubCrn, String currentVersion) {
        return new DistroXDatabaseUpgradeStatus(datahubCrn, "UPGRADE_NOT_REQUIRED", null, currentVersion);
    }

    public static DistroXDatabaseUpgradeStatus upgradeRequired(String datahubCrn, String targetVersion, String currentVersion) {
        return new DistroXDatabaseUpgradeStatus(datahubCrn, "UPGRADE_REQUIRED", targetVersion, currentVersion);
    }

    public static DistroXDatabaseUpgradeStatus unknown(String datahubCrn) {
        return new DistroXDatabaseUpgradeStatus(datahubCrn, "UNKNOWN", null, null);
    }

    public String getDatahubCrn() {
        return datahubCrn;
    }

    public void setDatahubCrn(String datahubCrn) {
        this.datahubCrn = datahubCrn;
    }

    @Override
    public String toString() {
        return "DistroXDatabaseUpgradeStatus{"
                + "datahubCrn='" + datahubCrn + '\''
                + ", upgradeStatus='" + getUpgradeStatus() + '\''
                + ", targetMajorVersion='" + getTargetMajorVersion() + '\''
                + ", currentMajorVersion='" + getCurrentMajorVersion() + '\''
                + ", eolDate='" + getEolDate() + '\''
                + '}';
    }
}
