package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;

import reactor.rx.Promise;

public class StackScaleTriggerEvent extends StackEvent {

    private Map<String, Set<Long>> hostGroupsWithPrivateIds;

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private String instanceGroup;

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private Integer adjustment;

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private Set<String> hostNames;

    private Map<String, Integer> hostGroupsWithAdjustment;

    private Map<String, Set<String>> hostGroupsWithHostNames;

    private final String triggeredStackVariant;

    private boolean repair;

    private NetworkScaleDetails networkScaleDetails;

    private final AdjustmentTypeWithThreshold adjustmentTypeWithThreshold;

    public StackScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment, Map<String, Set<Long>> hostGroupsWithPrivateIds,
            Map<String, Set<String>> hostGroupsWithHostNames, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant) {
        this(selector, stackId, hostGroupsWithAdjustment, hostGroupsWithPrivateIds, hostGroupsWithHostNames, NetworkScaleDetails.getEmpty(),
                adjustmentTypeWithThreshold, triggeredStackVariant);
    }

    public StackScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment, Map<String, Set<Long>> hostGroupsWithPrivateIds,
            Map<String, Set<String>> hostGroupsWithHostNames, NetworkScaleDetails networkScaleDetails, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold,
            String triggeredStackVariant) {
        super(selector, stackId);
        this.hostGroupsWithAdjustment = hostGroupsWithAdjustment;
        this.hostGroupsWithPrivateIds = hostGroupsWithPrivateIds;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames;
        this.networkScaleDetails = networkScaleDetails;
        this.adjustmentTypeWithThreshold = adjustmentTypeWithThreshold;
        this.triggeredStackVariant = triggeredStackVariant;
    }

    public StackScaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupsWithAdjustment, Map<String, Set<Long>> hostGroupsWithPrivateIds,
            Map<String, Set<String>> hostGroupsWithHostNames, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant,
            Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.hostGroupsWithAdjustment = hostGroupsWithAdjustment;
        this.hostGroupsWithPrivateIds = hostGroupsWithPrivateIds;
        this.hostGroupsWithHostNames = hostGroupsWithHostNames;
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
        if (hostGroupsWithAdjustment == null) {
            if (instanceGroup != null && adjustment != null) {
                hostGroupsWithAdjustment = Collections.singletonMap(instanceGroup, adjustment);
            } else {
                hostGroupsWithAdjustment = Collections.emptyMap();
            }
        }
        return hostGroupsWithAdjustment;
    }

    public Map<String, Set<String>> getHostGroupsWithHostNames() {
        if (hostGroupsWithHostNames == null) {
            if (instanceGroup != null && hostNames != null && !hostNames.isEmpty()) {
                hostGroupsWithHostNames = Collections.singletonMap(instanceGroup, hostNames);
            } else {
                hostGroupsWithHostNames = Collections.emptyMap();
            }
        }
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

    @Deprecated
    public String getInstanceGroup() {
        return instanceGroup;
    }

}
