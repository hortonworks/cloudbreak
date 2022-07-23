package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;

import reactor.rx.Promise;

public class ClusterDownscaleTriggerEvent extends ClusterScaleTriggerEvent {

    private final ClusterDownscaleDetails details;

    private final Set<String> zombieHostGroups;

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment,
            Map<String, Set<Long>> hostGroupWithPrivateIds, Map<String, Set<String>> hostGroupWithHostNames) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames);
        details = null;
        zombieHostGroups = null;
    }

    @JsonCreator
    public ClusterDownscaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroupsWithAdjustment") Map<String, Integer> hostGroupWithAdjustment,
            @JsonProperty("hostGroupsWithPrivateIds") Map<String, Set<Long>> hostGroupWithPrivateIds,
            @JsonProperty("hostGroupsWithHostNames") Map<String, Set<String>> hostGroupWithHostNames,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("details") ClusterDownscaleDetails details) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames, accepted);
        this.details = details;
        this.zombieHostGroups = null;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, Set<String> zombieHostGroups, Promise<AcceptResult> accepted,
            ClusterDownscaleDetails details) {
        super(selector, stackId, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), accepted);
        this.details = details;
        this.zombieHostGroups = zombieHostGroups;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }

    public Set<String> getZombieHostGroups() {
        return zombieHostGroups;
    }
}
