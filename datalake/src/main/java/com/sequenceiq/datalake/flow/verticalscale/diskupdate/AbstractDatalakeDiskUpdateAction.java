package com.sequenceiq.datalake.flow.verticalscale.diskupdate;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractDatalakeDiskUpdateAction<P extends ResourceCrnPayload>
        extends AbstractAction<DatalakeDiskUpdateState, DatalakeDiskUpdateStateSelectors, CommonContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDatalakeDiskUpdateAction.class);

    protected AbstractDatalakeDiskUpdateAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<DatalakeDiskUpdateState,
            DatalakeDiskUpdateStateSelectors> stateContext, P payload) {
        return new CommonContext(flowParameters);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
        return payload;
    }

    @Override
    protected void prepareExecution(P payload, Map<Object, Object> variables) {
        if (payload != null) {
            MdcContext.builder().resourceCrn(payload.getResourceCrn()).buildMdc();
        } else {
            LOGGER.warn("Payload was null in prepareExecution so resourceCrn cannot be added to the MdcContext!");
        }
    }
}
