package com.sequenceiq.datalake.flow.upgrade.database.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxUpgradeDatabaseServerEvent extends SdxEvent {

    private final TargetMajorVersion targetMajorVersion;

    private final boolean forced;

    @JsonCreator
    public SdxUpgradeDatabaseServerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("targetMajorVersion") TargetMajorVersion targetMajorVersion,
            @JsonProperty("forced") boolean forced) {
        super(selector, sdxId, userId);
        this.targetMajorVersion = targetMajorVersion;
        this.forced = forced;
    }

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxUpgradeDatabaseServerEvent.class, other,
                event -> targetMajorVersion == event.getTargetMajorVersion() && forced == event.isForced());
    }

    @Override
    public String toString() {
        return "SdxUpgradeDatabaseServerEvent{" +
                "targetMajorVersion=" + targetMajorVersion +
                ", forced=" + forced +
                "} " + super.toString();
    }

}
