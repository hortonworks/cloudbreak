package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.ScalingType;

import reactor.rx.Promise;

public class StackAndClusterUpscaleTriggerEvent extends StackScaleTriggerEvent {

    private final ScalingType scalingType;

    private final boolean singleMasterGateway;

    private final boolean kerberosSecured;

    private final boolean singleNodeCluster;

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, ScalingType scalingType) {
        super(selector, stackId, instanceGroup, adjustment, Collections.emptySet());
        this.scalingType = scalingType;
        this.singleMasterGateway = false;
        this.kerberosSecured = false;
        singleNodeCluster = false;
    }

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, ScalingType scalingType,
            Set<String> hostNames, boolean singlePrimaryGateway, boolean kerberosSecured, Promise<Boolean> accepted, boolean singleNodeCluster) {
        super(selector, stackId, instanceGroup, adjustment, hostNames, accepted);
        this.scalingType = scalingType;
        this.singleMasterGateway = singlePrimaryGateway;
        this.kerberosSecured = kerberosSecured;
        this.singleNodeCluster = singleNodeCluster;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public boolean isSingleMasterGateway() {
        return singleMasterGateway;
    }

    public boolean isKerberosSecured() {
        return kerberosSecured;
    }

    public boolean isSingleNodeCluster() {
        return singleNodeCluster;
    }
}
