package com.sequenceiq.redbeams.flow.redbeams.rotate;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerSuccess;

public enum RedbeamsSslCertRotateEvent implements FlowEvent {

    REDBEAMS_SSL_CERT_ROTATE_EVENT(),
    SSL_CERT_ROTATE_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(SslCertRotateDatabaseServerSuccess.class)),
    SSL_CERT_ROTATE_DATABASE_SERVER_FAILED_EVENT(EventSelectorUtil.selector(SslCertRotateDatabaseServerFailed.class)),
    REDBEAMS_SSL_CERT_ROTATE_FAILED_EVENT(),
    REDBEAMS_SSL_CERT_ROTATE_FINISHED_EVENT(),
    REDBEAMS_SSL_CERT_ROTATE_FAILURE_HANDLED_EVENT();

    private final String event;

    RedbeamsSslCertRotateEvent() {
        event = name();
    }

    RedbeamsSslCertRotateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
