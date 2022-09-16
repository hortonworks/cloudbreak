package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;

@Component("ProxyConfigModificationFinishedStateAction")
public class ProxyConfigModificationFinishedStateAction extends AbstractEnvProxyModificationAction<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigModificationFinishedStateAction.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    public ProxyConfigModificationFinishedStateAction(EnvironmentStatusUpdateService environmentStatusUpdateService) {
        super(EnvProxyModificationDefaultEvent.class);
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Override
    protected void doExecute(EnvProxyModificationContext context, EnvProxyModificationDefaultEvent payload, Map<Object, Object> variables) {
        LOGGER.info("Env proxy modification flow finished successfully");

        environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_FINISHED, EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FINISHED_STATE);
        sendEvent(context, EnvProxyModificationStateSelectors.FINALIZE_MODIFY_PROXY_EVENT.selector(), payload);
    }
}
