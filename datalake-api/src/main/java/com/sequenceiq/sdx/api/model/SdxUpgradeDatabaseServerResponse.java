package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxUpgradeDatabaseServerResponse {

    @ApiModelProperty(ModelDescriptions.DATABASE_SERVER_UPGRADE_RESPONSE_TYPE)
    private SdxDatabaseResponseType sdxDatabaseResponseType;

    @ApiModelProperty(ModelDescriptions.DATABASE_SERVER_UPGRADE_RESPONSE_REASON)
    private String reason;

    @ApiModelProperty(ModelDescriptions.FLOW_IDENTIFIER)
    private FlowIdentifier flowIdentifier;

    @ApiModelProperty(ModelDescriptions.DATABASE_SERVER_UPGRADE_TARGET_MAJOR_VERSION)
    private TargetMajorVersion targetMajorVersion;

    public SdxUpgradeDatabaseServerResponse() {
    }

    public SdxUpgradeDatabaseServerResponse(SdxDatabaseResponseType sdxDatabaseResponseType, FlowIdentifier flowIdentifier, String reason,
            TargetMajorVersion targetMajorVersion) {
        this.sdxDatabaseResponseType = sdxDatabaseResponseType;
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
        this.targetMajorVersion = targetMajorVersion;
    }

    public String getReason() {
        return reason;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public SdxDatabaseResponseType getSdxDatabaseResponseType() {
        return sdxDatabaseResponseType;
    }

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    @Override
    public String toString() {
        return "SdxUpgadeRdsResponse{" +
                "sdxDatabaseResponseType=" + sdxDatabaseResponseType +
                ", reason='" + reason + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                ", targetMajorVersion=" + targetMajorVersion +
                '}';
    }
}
