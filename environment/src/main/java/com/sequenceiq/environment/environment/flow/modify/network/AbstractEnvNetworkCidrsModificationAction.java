package com.sequenceiq.environment.environment.flow.modify.network;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractEnvNetworkCidrsModificationAction<P extends ResourceCrnPayload>
        extends AbstractAction<EnvNetworkCidrsModificationState, EnvNetworkCidrsModificationStateSelectors, CommonContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEnvNetworkCidrsModificationAction.class);

    protected AbstractEnvNetworkCidrsModificationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters,
            StateContext<EnvNetworkCidrsModificationState, EnvNetworkCidrsModificationStateSelectors> stateContext,
            P payload) {
        return new CommonContext(flowParameters);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
        LOGGER.debug("Received failure payload: {}", payload);
        return payload;
    }

    protected EnvironmentStatus getFailureEnvironmentStatus() {
        return EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_FAILED;
    }
}
