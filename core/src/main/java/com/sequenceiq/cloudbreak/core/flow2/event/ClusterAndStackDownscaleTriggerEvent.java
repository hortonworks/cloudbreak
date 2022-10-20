package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.common.type.ScalingType;

import reactor.rx.Promise;

public class ClusterAndStackDownscaleTriggerEvent extends ClusterDownscaleTriggerEvent {
    private final ScalingType scalingType;

    public ClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment, ScalingType scalingType) {
        super(selector, stackId, hostGroupWithAdjustment, Collections.emptyMap(), Collections.emptyMap());
        this.scalingType = scalingType;
    }

    public ClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment,
        Map<String, Set<Long>> hostGroupWithPrivateIds, Map<String, Set<String>> hostGroupWithHostNames, ScalingType scalingType) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames);
        this.scalingType = scalingType;
    }

    public ClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment,
                                                Map<String, Set<Long>> hostGroupWithPrivateIds, ScalingType scalingType,
                                                Promise<AcceptResult> accepted, ClusterDownscaleDetails details) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, Collections.emptyMap(), accepted, details);
        this.scalingType = scalingType;
    }

    public ClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Set<Long>> hostGroupWithPrivateIds, ScalingType scalingType,
                                                Promise<AcceptResult> accepted, ClusterDownscaleDetails details) {
        super(selector, stackId, Collections.emptyMap(), hostGroupWithPrivateIds, Collections.emptyMap(), accepted, details);
        this.scalingType = scalingType;
    }

    @JsonCreator
    public ClusterAndStackDownscaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroupsWithAdjustment") Map<String, Integer> hostGroupWithAdjustment,
            @JsonProperty("hostGroupsWithPrivateIds") Map<String, Set<Long>> hostGroupWithPrivateIds,
            @JsonProperty("hostGroupsWithHostNames") Map<String, Set<String>> hostGroupWithHostNames,
            @JsonProperty("scalingType") ScalingType scalingType,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("details") ClusterDownscaleDetails details) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames, accepted, details);
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
