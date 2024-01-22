package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeInitRequest extends StackEvent {

    private final String targetRuntimeVersion;

    @JsonCreator
    public ClusterUpgradeInitRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("targetRuntimeVersion") String targetRuntimeVersion) {
        super(stackId);
        this.targetRuntimeVersion = targetRuntimeVersion;
    }

    public String getTargetRuntimeVersion() {
        return targetRuntimeVersion;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterUpgradeInitRequest.class.getSimpleName() + "[", "]")
                .add("targetRuntimeVersion='" + targetRuntimeVersion + "'")
                .add(super.toString())
                .toString();
    }
}
