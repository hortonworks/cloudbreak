package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterCredentialChangeEvent implements FlowEvent {
    CLUSTER_CREDENTIALCHANGE_EVENT("CLUSTER_CREDENTIAL_CHANGE_TRIGGER_EVENT"),
    CLUSTER_CREDENTIALCHANGE_FINISHED_EVENT(EventSelectorUtil.selector(ClusterCredentialChangeResult.class)),
    CLUSTER_CREDENTIALCHANGE_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterCredentialChangeResult.class)),

    FINALIZED_EVENT("CLUSTERCREDENTIALCHANGEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERCREDENTIALCHANGEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERCREDENTIALCHANGEFAILHANDLEDEVENT");

    private final String event;

    ClusterCredentialChangeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
