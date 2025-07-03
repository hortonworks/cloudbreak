package com.sequenceiq.environment.environment.flow.hybrid.setup.action;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

abstract class AbstractEnvironmentCrossRealmTrustSetupAction<P extends ResourceCrnPayload>
        extends AbstractAction<EnvironmentCrossRealmTrustSetupState, EnvironmentCrossRealmTrustSetupStateSelectors, CommonContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEnvironmentCrossRealmTrustSetupAction.class);

    protected AbstractEnvironmentCrossRealmTrustSetupAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters,
            StateContext<EnvironmentCrossRealmTrustSetupState, EnvironmentCrossRealmTrustSetupStateSelectors> stateContext,
            P payload) {
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
