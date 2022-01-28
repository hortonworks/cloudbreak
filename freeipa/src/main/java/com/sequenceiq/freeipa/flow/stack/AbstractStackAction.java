package com.sequenceiq.freeipa.flow.stack;

import javax.annotation.PostConstruct;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractStackAction<S extends FlowState, E extends FlowEvent, C extends CommonContext, P extends Payload>
        extends AbstractAction<S, E, C, P> {

    protected AbstractStackAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @PostConstruct
    public void init() {
        super.init();
    }

    protected String getErrorReason(Exception payloadException) {
        return (payloadException == null || payloadException.getMessage() == null) ? "Unknown error" : payloadException.getMessage();
    }
}
