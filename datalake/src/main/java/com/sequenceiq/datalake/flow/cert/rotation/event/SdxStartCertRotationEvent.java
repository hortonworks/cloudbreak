package com.sequenceiq.datalake.flow.cert.rotation.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

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

    @JsonCreator
    public SdxStartCertRotationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("request") CertificatesRotationV4Request request) {
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
