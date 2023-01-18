package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.UPGRADE_DATABASE_SERVER_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpgradeDatabaseServerV4Request {

    @NotNull
    @Schema(description = ModelDescriptions.UpgradeModelDescriptions.MAJOR_VERSION, required = true)
    private UpgradeTargetMajorVersion upgradeTargetMajorVersion;

    public UpgradeTargetMajorVersion getUpgradeTargetMajorVersion() {
        return upgradeTargetMajorVersion;
    }

    public void setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion upgradeTargetMajorVersion) {
        this.upgradeTargetMajorVersion = upgradeTargetMajorVersion;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerV4Request{" +
                "majorVersion=" + upgradeTargetMajorVersion +
                '}';
    }
}
