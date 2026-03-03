package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.model.OsType;

public class ClusterUpgradeInitRequest extends StackEvent {

    private final String targetRuntimeVersion;

    private final OsType targetOsType;

    private final String architecture;

    private final OsType originalOsType;

    @JsonCreator
    public ClusterUpgradeInitRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("targetRuntimeVersion") String targetRuntimeVersion,
            @JsonProperty("targetOsType") OsType targetOsType,
            @JsonProperty("architecture") String architecture,
            @JsonProperty("originalOsType") OsType originalOsType) {
        super(stackId);
        this.targetRuntimeVersion = targetRuntimeVersion;
        this.targetOsType = targetOsType;
        this.architecture = architecture;
        this.originalOsType = originalOsType;
    }

    public String getTargetRuntimeVersion() {
        return targetRuntimeVersion;
    }

    public OsType getOriginalOsType() {
        return originalOsType;
    }

    public OsType getTargetOsType() {
        return targetOsType;
    }

    public String getArchitecture() {
        return architecture;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterUpgradeInitRequest.class.getSimpleName() + "[", "]")
                .add("targetRuntimeVersion='" + targetRuntimeVersion + "'")
                .add("targetOsType='" + targetOsType + "'")
                .add("architecture='" + architecture + "'")
                .add("originalOsType='" + originalOsType + "'")
                .add(super.toString())
                .toString();
    }
}
