package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.ProxyConfigModificationEvent;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractEnvProxyModificationAction<P extends ProxyConfigModificationEvent>
        extends AbstractAction<EnvProxyModificationState, EnvProxyModificationStateSelectors, EnvProxyModificationContext, P> {

    public static final String PROXY_CONFIG = "proxyConfig";

    public static final String PREVIOUS_PROXY_CONFIG = "previousProxyConfig";

    @Inject
    private ProxyConfigService proxyConfigService;

    protected AbstractEnvProxyModificationAction(Class<P> clazz) {
        super(clazz);
    }

    @Override
    protected EnvProxyModificationContext createFlowContext(FlowParameters flowParameters,
            StateContext<EnvProxyModificationState, EnvProxyModificationStateSelectors> stateContext, P payload) {
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        ProxyConfig proxyConfig = (ProxyConfig) variables.computeIfAbsent(PROXY_CONFIG, k -> getProxyConfig(payload));
        ProxyConfig previousProxyConfig = (ProxyConfig) variables.computeIfAbsent(PREVIOUS_PROXY_CONFIG, k -> getPreviousProxyConfig(payload));
        return new EnvProxyModificationContext(flowParameters, proxyConfig, previousProxyConfig);
    }

    private ProxyConfig getProxyConfig(P payload) {
        return payload.getProxyConfigCrn() != null
                ? proxyConfigService.getByCrn(payload.getProxyConfigCrn())
                : null;
    }

    private ProxyConfig getPreviousProxyConfig(P payload) {
        return proxyConfigService.getOptionalByEnvironmentCrn(payload.getResourceCrn()).orElse(null);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<EnvProxyModificationContext> flowContext, Exception ex) {
        return new EnvProxyModificationFailedEvent(payload, ex, getFailureEnvironmentStatus());
    }

    protected EnvironmentStatus getFailureEnvironmentStatus() {
        return EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED;
    }

    protected String getProxyConfigName(EnvProxyModificationContext context) {
        return context.getProxyConfig() != null ? context.getProxyConfig().getName() : "no proxy";
    }
}
