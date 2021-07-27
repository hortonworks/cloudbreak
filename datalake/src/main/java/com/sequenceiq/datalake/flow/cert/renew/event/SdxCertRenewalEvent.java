package com.sequenceiq.datalake.flow.cert.renew.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SdxCertRenewalEvent implements FlowEvent {
    RENEW_CERT_EVENT(SdxStartCertRenewalEvent.class),
    CERT_RENEWAL_STARTED_EVENT,
    CERT_RENEWAL_FINISHED_EVENT,
    CERT_RENEWAL_FINALIZED_EVENT,
    CERT_RENEWAL_FAILED_EVENT(SdxCertRenewalFailedEvent.class),
    CERT_RENEWAL_FAILURE_HANDLED_EVENT;

    private final String event;

    SdxCertRenewalEvent() {
        event = name();
    }

    SdxCertRenewalEvent(Class<?> clazz) {
        event = EventSelectorUtil.selector(clazz);
    }

    @Override
    public String event() {
        return event;
    }
}
