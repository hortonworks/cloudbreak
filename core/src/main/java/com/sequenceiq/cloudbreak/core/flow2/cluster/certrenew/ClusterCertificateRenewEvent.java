package com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRedeploySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateReissueSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRenewFailed;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterCertificateRenewEvent implements FlowEvent {
    CLUSTER_CERTIFICATE_REISSUE_EVENT("CLUSTER_CERTIFICATE_REISSUE_EVENT"),
    CLUSTER_CERTIFICATES_REDEPLOY_EVENT(EventSelectorUtil.selector(ClusterCertificateReissueSuccess.class)),
    CLUSTER_CERTIFICATES_REDEPLOY_FINISHED_EVENT(EventSelectorUtil.selector(ClusterCertificateRedeploySuccess.class)),
    CLUSTER_CERTIFICATE_RENEW_FAILED_EVENT(EventSelectorUtil.selector(ClusterCertificateRenewFailed.class)),
    CLUSTER_CERTIFICATE_RENEW_FINISHED_EVENT("CLUSTER_CERTIFICATE_RENEW_FINISHED_EVENT"),
    CLUSTER_CERTIFICATE_RENEW_FAILURE_HANDLED_EVENT("CLUSTER_CERTIFICATE_RENEW_FAILURE_HANDLED_EVENT");

    private final String event;

    ClusterCertificateRenewEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
