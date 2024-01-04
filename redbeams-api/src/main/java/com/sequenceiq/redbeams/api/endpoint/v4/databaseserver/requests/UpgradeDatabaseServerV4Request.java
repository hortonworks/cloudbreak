package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.UPGRADE_DATABASE_SERVER_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpgradeDatabaseServerV4Request {

    @NotNull
    @Schema(description = ModelDescriptions.UpgradeModelDescriptions.MAJOR_VERSION, required = true)
    private UpgradeTargetMajorVersion upgradeTargetMajorVersion;

    @Schema(description = ModelDescriptions.UpgradeModelDescriptions.UPGRADED_DATABASE_SETTINGS)
    private DatabaseServerV4StackRequest upgradedDatabaseSettings;

    public UpgradeTargetMajorVersion getUpgradeTargetMajorVersion() {
        return upgradeTargetMajorVersion;
    }

    public void setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion upgradeTargetMajorVersion) {
        this.upgradeTargetMajorVersion = upgradeTargetMajorVersion;
    }

    public DatabaseServerV4StackRequest getUpgradedDatabaseSettings() {
        return upgradedDatabaseSettings;
    }

    public void setUpgradedDatabaseSettings(DatabaseServerV4StackRequest upgradedDatabaseSettings) {
        this.upgradedDatabaseSettings = upgradedDatabaseSettings;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerV4Request{" +
                "upgradeTargetMajorVersion=" + upgradeTargetMajorVersion +
                ", upgradedDatabaseSettings=" + upgradedDatabaseSettings +
                '}';
    }
}
