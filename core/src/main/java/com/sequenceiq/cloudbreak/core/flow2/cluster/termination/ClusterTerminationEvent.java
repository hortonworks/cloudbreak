package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.PrepareClusterTerminationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;

public enum ClusterTerminationEvent implements FlowEvent {
    TERMINATION_EVENT("CLUSTER_TERMINATION_TRIGGER_EVENT"),
    PROPER_TERMINATION_EVENT("PROPER_TERMINATION_EVENT"),
    RECOVERY_TERMINATION_EVENT("CLUSTER_RECOVERY_TERMINATION_EVENT"),
    PREPARE_CLUSTER_FINISHED_EVENT(EventSelectorUtil.selector(PrepareClusterTerminationResult.class)),
    PREPARE_CLUSTER_FAILED_EVENT(EventSelectorUtil.failureSelector(PrepareClusterTerminationResult.class)),
    DEREGISTER_SERVICES_FINISHED_EVENT(EventSelectorUtil.selector(DeregisterServicesResult.class)),
    DEREGISTER_SERVICES_FAILED_EVENT(EventSelectorUtil.failureSelector(DeregisterServicesResult.class)),
    DISABLE_KERBEROS_FINISHED_EVENT(EventSelectorUtil.selector(DisableKerberosResult.class)),
    DISABLE_KERBEROS_FAILED_EVENT(EventSelectorUtil.failureSelector(DisableKerberosResult.class)),
    TERMINATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterTerminationResult.class)),
    TERMINATION_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterTerminationResult.class)),

    FINALIZED_EVENT("TERMINATECLUSTERFINALIZED"),
    FAILURE_EVENT("TERMINATECLUSTERFAILUREEVENT"),
    FAIL_HANDLED_EVENT("TERMINATECLUSTERFAILHANDLED");

    private final String event;

    ClusterTerminationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
