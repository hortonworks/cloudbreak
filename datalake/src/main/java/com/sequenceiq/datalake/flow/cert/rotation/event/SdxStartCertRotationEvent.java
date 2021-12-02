package com.sequenceiq.datalake.flow.cert.rotation.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

import reactor.rx.Promise;

public class SdxStartCertRotationEvent extends SdxEvent {

    private final CertificatesRotationV4Request request;

    public SdxStartCertRotationEvent(Long sdxId, String userId, CertificatesRotationV4Request request) {
        super(sdxId, userId);
        this.request = request;
    }

    public SdxStartCertRotationEvent(SdxContext context, CertificatesRotationV4Request request) {
        super(context);
        this.request = request;
    }

    public SdxStartCertRotationEvent(String selector, Long sdxId, String userId, CertificatesRotationV4Request request) {
        super(selector, sdxId, userId);
        this.request = request;
    }

    public SdxStartCertRotationEvent(String selector, SdxContext context, CertificatesRotationV4Request request) {
        super(selector, context);
        this.request = request;
    }

    public SdxStartCertRotationEvent(String selector, Long sdxId, String userId, Promise<AcceptResult> accepted, CertificatesRotationV4Request request) {
        super(selector, sdxId, userId, accepted);
        this.request = request;
    }

    public CertificatesRotationV4Request getRequest() {
        return request;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxStartCertRotationEvent.class, other,
                event -> Objects.equals(request, event.request));
    }
}
