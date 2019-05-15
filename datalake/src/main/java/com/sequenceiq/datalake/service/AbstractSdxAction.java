package com.sequenceiq.datalake.service;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractSdxAction<P extends Payload> extends AbstractAction<FlowState, FlowEvent, SdxContext, P> {

    protected AbstractSdxAction(Class<P> payloadClass) {
        super(payloadClass);
    }
}
