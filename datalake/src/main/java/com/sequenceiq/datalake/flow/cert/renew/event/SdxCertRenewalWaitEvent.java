package com.sequenceiq.datalake.flow.cert.renew.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCertRenewalWaitEvent extends SdxEvent {

    @JsonCreator
    public SdxCertRenewalWaitEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxName") String sdxName,
            @JsonProperty("userId") String userId) {
        super(selector, sdxId, sdxName, userId);
    }

    public SdxCertRenewalWaitEvent(SdxContext context) {
        super(context);
    }
}
