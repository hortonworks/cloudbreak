package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;

import reactor.rx.Promise;

public class StackScaleTriggerEvent extends StackEvent {

    private final String instanceGroup;

    private final Integer adjustment;

    private final Set<String> hostNames;

    private final String triggeredStackVariant;

    private boolean repair;

    private NetworkScaleDetails networkScaleDetails;

    private final AdjustmentTypeWithThreshold adjustmentTypeWithThreshold;

    public StackScaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant) {
        this(selector, stackId, instanceGroup, adjustment, Collections.emptySet(), NetworkScaleDetails.getEmpty(), adjustmentTypeWithThreshold,
                triggeredStackVariant);
    }

    public StackScaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant, Promise<AcceptResult> accepted) {
        this(selector, stackId, instanceGroup, adjustment, Collections.emptySet(), adjustmentTypeWithThreshold, triggeredStackVariant, accepted);
    }

    public StackScaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, Set<String> hostNames,
            NetworkScaleDetails networkScaleDetails, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant) {
        super(selector, stackId);
        this.instanceGroup = instanceGroup;
        this.adjustment = adjustment;
        this.hostNames = hostNames;
        this.networkScaleDetails = networkScaleDetails;
        this.adjustmentTypeWithThreshold = adjustmentTypeWithThreshold;
        this.triggeredStackVariant = triggeredStackVariant;
    }

    public StackScaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, Set<String> hostNames,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.instanceGroup = instanceGroup;
        this.adjustment = adjustment;
        this.hostNames = hostNames;
        this.networkScaleDetails = new NetworkScaleDetails();
        this.adjustmentTypeWithThreshold = adjustmentTypeWithThreshold;
        this.triggeredStackVariant = triggeredStackVariant;
    }

    public StackScaleTriggerEvent setRepair() {
        repair = true;
        return this;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public Set<String> getHostNames() {
        return hostNames;
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
