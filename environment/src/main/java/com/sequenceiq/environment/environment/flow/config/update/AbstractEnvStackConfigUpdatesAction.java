package com.sequenceiq.environment.environment.flow.config.update;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

abstract class AbstractEnvStackConfigUpdatesAction<P extends ResourceCrnPayload> extends
    AbstractAction<EnvStackConfigUpdatesState, EnvStackConfigUpdatesStateSelectors, CommonContext, P> {

    private static final Logger LOGGER = LoggerFactory
        .getLogger(AbstractEnvStackConfigUpdatesAction.class);

    AbstractEnvStackConfigUpdatesAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters,
        StateContext<EnvStackConfigUpdatesState, EnvStackConfigUpdatesStateSelectors> stateContext,
        P payload) {
        return new CommonContext(flowParameters);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext,
        Exception ex) {
        return payload;
    }

    @Override
    protected void prepareExecution(P payload, Map<Object, Object> variables) {
        if (payload != null) {
            MdcContext.builder().resourceCrn(payload.getResourceCrn()).buildMdc();
        } else {
            LOGGER.warn(
                "Payload was null in prepareExecution so resourceCrn cannot be added to the MdcContext!");
        }
    }
}
