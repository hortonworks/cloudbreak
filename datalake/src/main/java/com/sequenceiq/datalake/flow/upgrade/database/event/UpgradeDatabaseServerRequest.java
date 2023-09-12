package com.sequenceiq.datalake.flow.upgrade.database.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeDatabaseServerRequest extends SdxEvent {

    private final TargetMajorVersion targetMajorVersion;

    private final boolean forced;

    @JsonCreator
    public UpgradeDatabaseServerRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("targetMajorVersion") TargetMajorVersion targetMajorVersion,
            @JsonProperty("forced") boolean forced) {
        super(sdxId, userId);
        this.targetMajorVersion = targetMajorVersion;
        this.forced = forced;
    }

    public static UpgradeDatabaseServerRequest from(SdxContext context, TargetMajorVersion targetMajorVersion, boolean forced) {
        return new UpgradeDatabaseServerRequest(context.getSdxId(), context.getUserId(), targetMajorVersion, forced);
    }

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerRequest{" +
                "targetMajorVersion=" + targetMajorVersion +
                ", forced=" + forced +
                "} " + super.toString();
    }

}