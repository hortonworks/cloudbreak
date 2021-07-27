package com.sequenceiq.datalake.flow.cert.renew.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCertRenewalWaitEvent extends SdxEvent {
    public SdxCertRenewalWaitEvent(SdxContext context) {
        super(context);
    }
}
