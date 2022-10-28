package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = ModelDescriptions.UPGRADE_DATABASE_SERVER_RESPONSE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpgradeDatabaseServerV4Response {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.UpgradeModelDescriptions.CURRENT_VERSION)
    private MajorVersion currentVersion;

    @ApiModelProperty(value = ModelDescriptions.UpgradeModelDescriptions.UPGRADE_REASON)
    private String reason;

    @ApiModelProperty(value = ModelDescriptions.FLOW_IDENTIFIER)
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
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
