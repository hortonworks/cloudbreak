package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.GetLatestRdsCertificateResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RestartCmResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RollingRestartServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateCheckPrerequisitesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateOnProviderResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateLatestRdsCertificateResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateTlsRdsResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum RotateRdsCertificateEvent implements FlowEvent {
    ROTATE_RDS_CERTIFICATE_EVENT("ROTATE_RDS_CERTIFICATE_TRIGGER_EVENT"),
    ROTATE_RDS_CERTIFICATE_FAILED_EVENT(EventSelectorUtil.selector(RotateRdsCertificateFailedEvent.class)),
    ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_FINISHED_EVENT(EventSelectorUtil.selector(RotateRdsCertificateCheckPrerequisitesResult.class)),
    ROTATE_RDS_CERTIFICATE_TLS_SETUP_FINISHED_EVENT(EventSelectorUtil.selector(UpdateTlsRdsResult.class)),
    GET_LATEST_RDS_CERTIFICATE_FINISHED_EVENT(EventSelectorUtil.selector(GetLatestRdsCertificateResult.class)),
    UPDATE_TO_LATEST_RDS_CERTIFICATE_FINISHED_EVENT(EventSelectorUtil.selector(UpdateLatestRdsCertificateResult.class)),
    CM_RESTART_FINISHED_EVENT(EventSelectorUtil.selector(RestartCmResult.class)),
    ROLLING_RESTART_SERVICES_FINISHED_EVENT(EventSelectorUtil.selector(RollingRestartServicesResult.class)),
    ROTATE_RDS_CERTIFICATE_ON_PROVIDER_FINISHED_EVENT(EventSelectorUtil.selector(RotateRdsCertificateOnProviderResult.class)),
    FINALIZED_EVENT("ROTATE_RDS_CERTIFICATE_FINALIZED_EVENT"),
    FAIL_HANDLED_EVENT("ROTATE_RDS_CERTIFICATE_FAIL_HANDLED_EVENT");

    private final String event;

    RotateRdsCertificateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
