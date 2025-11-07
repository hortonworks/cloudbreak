package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustSetupCommandsV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustSetupCommandsResponse extends TrustSetupCommandsBase {
    @NotEmpty
    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.KDC_TYPE, requiredMode = Schema.RequiredMode.REQUIRED)
    private String kdcType;

    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.AD_TRUST_SETUP_COMMANDS, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private ActiveDirectoryTrustSetupCommands activeDirectoryCommands;

    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.MIT_TRUST_SETUP_COMMANDS, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private MitTrustSetupCommands mitCommands;

    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.BASE_CLUSTER_KRB5_CONF, requiredMode = Schema.RequiredMode.REQUIRED)
    private BaseClusterTrustSetupCommands baseClusterCommands;

    public String getKdcType() {
        return kdcType;
    }

    public void setKdcType(String kdcType) {
        this.kdcType = kdcType;
    }

    public ActiveDirectoryTrustSetupCommands getActiveDirectoryCommands() {
        return activeDirectoryCommands;
    }

    public void setActiveDirectoryCommands(ActiveDirectoryTrustSetupCommands activeDirectoryTrustSetupCommands) {
        this.activeDirectoryCommands = activeDirectoryTrustSetupCommands;
    }

    public MitTrustSetupCommands getMitCommands() {
        return mitCommands;
    }

    public void setMitCommands(MitTrustSetupCommands mitCommands) {
        this.mitCommands = mitCommands;
    }

    public BaseClusterTrustSetupCommands getBaseClusterCommands() {
        return baseClusterCommands;
    }

    public void setBaseClusterCommands(BaseClusterTrustSetupCommands baseClusterTrustSetupCommands) {
        this.baseClusterCommands = baseClusterTrustSetupCommands;
    }

    @Override
    public String toString() {
        return "TrustSetupCommandsResponse{" +
                "kdcType='" + kdcType + '\'' +
                ", activeDirectoryCommands=" + activeDirectoryCommands +
                ", mitCommands=" + mitCommands +
                ", baseClusterCommands=" + baseClusterCommands +
                "} " + super.toString();
    }
}
