package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.UPGRADE_DATABASE_SERVER_RESPONSE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpgradeDatabaseServerV4Response {

    @NotNull
    @Schema(description = ModelDescriptions.UpgradeModelDescriptions.CURRENT_VERSION)
    private MajorVersion currentVersion;

    @Schema(description = ModelDescriptions.UpgradeModelDescriptions.UPGRADE_REASON)
    private String reason;

    @Schema(description = ModelDescriptions.UpgradeModelDescriptions.UPGRADE_REASON_WARNING)
    private boolean warning;

    @Schema(description = ModelDescriptions.FLOW_IDENTIFIER)
    private FlowIdentifier flowIdentifier;

    public MajorVersion getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(MajorVersion currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isWarning() {
        return warning;
    }

    public void setWarning(boolean warning) {
        this.warning = warning;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerV4Response{" +
                "currentVersion=" + currentVersion +
                ", reason='" + reason + '\'' +
                ", warning=" + warning +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
