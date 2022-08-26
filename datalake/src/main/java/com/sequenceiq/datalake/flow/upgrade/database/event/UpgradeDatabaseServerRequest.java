package com.sequenceiq.datalake.flow.upgrade.database.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeDatabaseServerRequest extends SdxEvent {

    private final TargetMajorVersion targetMajorVersion;

    @JsonCreator
    public UpgradeDatabaseServerRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("targetMajorVersion") TargetMajorVersion targetMajorVersion) {
        super(sdxId, userId);
        this.targetMajorVersion = targetMajorVersion;
    }

    public static UpgradeDatabaseServerRequest from(SdxContext context, TargetMajorVersion targetMajorVersion) {
        return new UpgradeDatabaseServerRequest(context.getSdxId(), context.getUserId(), targetMajorVersion);
    }

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerRequest{" +
                "targetMajorVersion=" + targetMajorVersion +
                "} " + super.toString();
    }
}

