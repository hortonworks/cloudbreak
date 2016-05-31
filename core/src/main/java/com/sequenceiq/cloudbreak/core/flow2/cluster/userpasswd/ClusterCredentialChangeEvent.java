package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd;

import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeResult;

public enum ClusterCredentialChangeEvent implements FlowEvent {
    CLUSTER_CREDENTIALCHANGE_EVENT(FlowPhases.CLUSTER_USERNAME_PASSWORD_UPDATE.name()),
    CLUSTER_CREDENTIALCHANGE_FINISHED_EVENT(EventSelectorUtil.selector(ClusterCredentialChangeResult.class)),
    CLUSTER_CREDENTIALCHANGE_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterCredentialChangeResult.class)),

    FINALIZED_EVENT("CLUSTERCREDENTIALCHANGEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERCREDENTIALCHANGEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERCREDENTIALCHANGEFAILHANDLEDEVENT");

    private String stringRepresentation;

    ClusterCredentialChangeEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
