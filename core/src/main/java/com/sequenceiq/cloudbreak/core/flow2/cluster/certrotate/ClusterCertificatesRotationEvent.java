package com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCMCARotationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCertificatesRotationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterCertificatesRotationEvent implements FlowEvent {
    CLUSTER_CMCA_ROTATION_EVENT("CLUSTER_CERTIFICATES_ROTATION_EVENT"),
    CLUSTER_HOST_CERTIFICATES_ROTATION_EVENT(EventSelectorUtil.selector(ClusterCMCARotationSuccess.class)),
    CLUSTER_HOST_CERTIFICATES_ROTATION_FINISHED_EVENT(EventSelectorUtil.selector(ClusterHostCertificatesRotationSuccess.class)),
    CLUSTER_CERTIFICATES_ROTATION_FAILED_EVENT(EventSelectorUtil.selector(ClusterCertificatesRotationFailed.class)),
    CLUSTER_CERTIFICATES_ROTATION_FINISHED_EVENT("CLUSTER_CERTIFICATES_ROTATION_FINISHED_EVENT"),
    CLUSTER_CERTIFICATES_ROTATION_FAILURE_HANDLED_EVENT("CLUSTER_CERTIFICATES_ROTATION_FAILURE_HANDLED_EVENT");

    private final String event;

    ClusterCertificatesRotationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
