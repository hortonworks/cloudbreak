package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MultiHostgroupClusterAndStackDownscaleTriggerEvent extends StackEvent {
    private final ScalingType scalingType;

    private final Map<String, Set<Long>> privateIdsByHostgroupMap;

    private final ClusterDownscaleDetails details;

    @JsonCreator
    public MultiHostgroupClusterAndStackDownscaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("privateIdsByHostgroupMap") Map<String, Set<Long>> privateIdsByHostgroupMap,
            @JsonProperty("details") ClusterDownscaleDetails details,
            @JsonProperty("scalingType") ScalingType scalingType,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.privateIdsByHostgroupMap = privateIdsByHostgroupMap;
        this.details = details;
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public Map<String, Set<Long>> getPrivateIdsByHostgroupMap() {
        return privateIdsByHostgroupMap;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }
}
