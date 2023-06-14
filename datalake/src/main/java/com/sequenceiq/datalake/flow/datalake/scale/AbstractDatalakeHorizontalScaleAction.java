package com.sequenceiq.datalake.flow.datalake.scale;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractDatalakeHorizontalScaleAction<P extends Payload>
        extends AbstractAction<DatalakeHorizontalScaleState, DatalakeHorizontalScaleEvent, CommonContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDatalakeHorizontalScaleAction.class);

    protected AbstractDatalakeHorizontalScaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters,
            StateContext<DatalakeHorizontalScaleState, DatalakeHorizontalScaleEvent> stateContext,
            P payload) {
        return new CommonContext(flowParameters);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
        LOGGER.error("Datalake horizontal scale failed with exception.", ex);
        return payload;
    }
}
