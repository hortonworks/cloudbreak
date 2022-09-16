package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;

@Component("ProxyConfigModificationFailedStateAction")
public class ProxyConfigModificationFailedStateAction extends AbstractEnvProxyModificationAction<EnvProxyModificationFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigModificationFailedStateAction.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    public ProxyConfigModificationFailedStateAction(EnvironmentStatusUpdateService environmentStatusUpdateService) {
        super(EnvProxyModificationFailedEvent.class);
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Override
    protected void doExecute(EnvProxyModificationContext context, EnvProxyModificationFailedEvent payload, Map<Object, Object> variables) {
        LOGGER.warn("Env proxy modification flow finished with an error", payload.getException());

        environmentStatusUpdateService.updateFailedEnvironmentStatusAndNotify(context, payload, payload.getEnvironmentStatus(),
                ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_FAILED, List.of(payload.getException().getMessage()),
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FAILED_STATE);
        sendEvent(context, EnvProxyModificationStateSelectors.HANDLE_FAILED_MODIFY_PROXY_EVENT.event(), payload);
    }
}
