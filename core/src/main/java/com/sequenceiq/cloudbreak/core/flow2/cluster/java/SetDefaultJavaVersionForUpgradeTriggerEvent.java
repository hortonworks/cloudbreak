package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;

public class SetDefaultJavaVersionForUpgradeTriggerEvent extends SetDefaultJavaVersionTriggerEvent {

    @JsonCreator
    public SetDefaultJavaVersionForUpgradeTriggerEvent(@JsonProperty("resourceId") Long resourceId) {
        super(FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT, resourceId, null, false, false, false);
    }

    @Override
    public String toString() {
        return "SetDefaultJavaVersionForUpgradeTriggerEvent{resourceId=" + getResourceId() + "} " + super.toString();
    }
}
