package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;

public class StackScaleTriggerEvent extends StackEvent {

    private Map<String, Set<Long>> hostGroupsWithPrivateIds;

    private final Map<String, Integer> hostGroupsWithAdjustment;

    private final Map<String, Set<String>> hostGroupsWithHostNames;

    private final String triggeredStackVariant;

    private boolean repair;

    private final NetworkScaleDetails networkScaleDetails;

    private final AdjustmentTypeWithThreshold adjustmentTypeWithThreshold;

    public StackScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment, Map<String, Set<Long>> hostGroupsWithPrivateIds,
        Map<String, Set<String>> hostGroupsWithHostNames, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant) {
        this(selector, stackId, hostGroupsWithAdjustment, hostGroupsWithPrivateIds, hostGroupsWithHostNames, NetworkScaleDetails.getEmpty(),
                adjustmentTypeWithThreshold, triggeredStackVariant);
    }

    @JsonCreator
    public StackScaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroupsWithAdjustment") Map<String, Integer> hostGroupsWithAdjustment,
            @JsonProperty("hostGroupsWithPrivateIds") Map<String, Set<Long>> hostGroupsWithPrivateIds,
            @JsonProperty("hostGroupsWithHostNames") Map<String, Set<String>> hostGroupsWithHostNames,
            @JsonProperty("networkScaleDetails") NetworkScaleDetails networkScaleDetails,
            @JsonProperty("adjustmentTypeWithThreshold") AdjustmentTypeWithThreshold adjustmentTypeWithThreshold,
            @JsonProperty("triggeredStackVariant") String triggeredStackVariant) {

        super(selector, stackId);
        this.hostGroupsWithAdjustment = hostGroupsWithAdjustment == null ? Collections.emptyMap() : hostGroupsWithAdjustment;
        this.hostGroupsWithPrivateIds = hostGroupsWithPrivateIds;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames == null ? Collections.emptyMap() : hostGroupsWithHostNames;
        this.networkScaleDetails = networkScaleDetails;
        this.adjustmentTypeWithThreshold = adjustmentTypeWithThreshold;
        this.triggeredStackVariant = triggeredStackVariant;
    }

    public StackScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment, Map<String, Set<Long>> hostGroupsWithPrivateIds,
        Map<String, Set<String>> hostGroupsWithHostNames, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant,
        Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.hostGroupsWithAdjustment = hostGroupsWithAdjustment == null ? Collections.emptyMap() : hostGroupsWithAdjustment;
        this.hostGroupsWithPrivateIds = hostGroupsWithPrivateIds;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames == null ? Collections.emptyMap() : hostGroupsWithHostNames;
        this.networkScaleDetails = new NetworkScaleDetails();
        this.adjustmentTypeWithThreshold = adjustmentTypeWithThreshold;
        this.triggeredStackVariant = triggeredStackVariant;
    }

    public StackScaleTriggerEvent setRepair() {
        repair = true;
        return this;
    }

    public void setHostGroupsWithPrivateIds(Map<String, Set<Long>> hostGroupsWithPrivateIds) {
        this.hostGroupsWithPrivateIds = hostGroupsWithPrivateIds;
    }

    public Map<String, Set<Long>> getHostGroupsWithPrivateIds() {
        if (hostGroupsWithPrivateIds == null) {
            hostGroupsWithPrivateIds = Collections.emptyMap();
        }
        return hostGroupsWithPrivateIds;
    }

    public Map<String, Integer> getHostGroupsWithAdjustment() {
        return hostGroupsWithAdjustment;
    }

    public Map<String, Set<String>> getHostGroupsWithHostNames() {
        return hostGroupsWithHostNames;
    }

    public boolean isRepair() {
        return repair;
    }

    public AdjustmentTypeWithThreshold getAdjustmentTypeWithThreshold() {
        return adjustmentTypeWithThreshold;
    }

    public NetworkScaleDetails getNetworkScaleDetails() {
        return networkScaleDetails;
    }

    public String getTriggeredStackVariant() {
        return triggeredStackVariant;
    }

}
