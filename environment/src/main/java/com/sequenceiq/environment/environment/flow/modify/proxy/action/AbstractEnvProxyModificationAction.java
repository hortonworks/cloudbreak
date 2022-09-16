package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.EnvironmentEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractEnvProxyModificationAction<P extends EnvironmentEvent>
        extends AbstractAction<EnvProxyModificationState, EnvProxyModificationStateSelectors, EnvProxyModificationContext, P> {

    public static final String PREVIOUS_PROXY_CONFIG = "previousProxyConfig";

    @Inject
    private EnvironmentService environmentService;

    protected AbstractEnvProxyModificationAction(Class<P> clazz) {
        super(clazz);
    }

    @Override
    protected EnvProxyModificationContext createFlowContext(FlowParameters flowParameters,
            StateContext<EnvProxyModificationState, EnvProxyModificationStateSelectors> stateContext, P payload) {
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        ProxyConfig previousProxyConfig = (ProxyConfig) variables.computeIfAbsent(PREVIOUS_PROXY_CONFIG, k -> getPreviousProxyConfig(payload));
        return new EnvProxyModificationContext(flowParameters, previousProxyConfig);
    }

    private ProxyConfig getPreviousProxyConfig(P payload) {
        return environmentService.findEnvironmentByIdOrThrow(payload.getResourceId()).getProxyConfig();
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<EnvProxyModificationContext> flowContext, Exception ex) {
        return new EnvProxyModificationFailedEvent(payload.getEnvironmentDto(), getFailureEnvironmentStatus(), ex);
    }

    protected EnvironmentStatus getFailureEnvironmentStatus() {
        return EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED;
    }
}
