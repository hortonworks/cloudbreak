package com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister;

import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterProxyReRegistrationEvent implements FlowEvent {
    CLUSTER_PROXY_RE_REGISTRATION_EVENT("CLUSTER_PROXY_RE_REGISTRATION_EVENT"),
    CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterProxyReRegistrationResult.class)),
    CLUSTER_PROXY_RE_REGISTRATION_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterProxyReRegistrationResult.class)),
    CLUSTER_PROXY_RE_REGISTRATION_FAIL_HANDLED_EVENT("CLUSTER_PROXY_RE_REGISTRATION_FAIL_HANDLED_EVENT");

    private final String event;

    ClusterProxyReRegistrationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
