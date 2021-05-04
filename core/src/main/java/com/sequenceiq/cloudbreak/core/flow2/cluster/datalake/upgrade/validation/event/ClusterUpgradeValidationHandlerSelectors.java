package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ClusterUpgradeValidationHandlerSelectors implements FlowEvent {

    VALIDATE_CLUSTER_EVENT;

    @Override
    public String event() {
        return name();
    }

}
