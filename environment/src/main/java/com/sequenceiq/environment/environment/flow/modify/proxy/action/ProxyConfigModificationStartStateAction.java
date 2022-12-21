package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;

@Component("ProxyConfigModificationStartStateAction")
public class ProxyConfigModificationStartStateAction extends AbstractEnvProxyModificationAction<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigModificationStartStateAction.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    public ProxyConfigModificationStartStateAction(EnvironmentStatusUpdateService environmentStatusUpdateService) {
        super(EnvProxyModificationDefaultEvent.class);
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Override
    protected void doExecute(EnvProxyModificationContext context, EnvProxyModificationDefaultEvent payload, Map<Object, Object> variables) {
        LOGGER.info("Env proxy modification flow started");

        environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                EnvironmentStatus.PROXY_CONFIG_MODIFICATION_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_STARTED, List.of(getProxyConfigName(context)),
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_START_STATE);

        String selector = EnvProxyModificationHandlerSelectors.SAVE_NEW_PROXY_ASSOCIATION_HANDLER_EVENT.selector();
        EnvProxyModificationDefaultEvent event = EnvProxyModificationDefaultEvent.builder()
                .withSelector(selector)
                .withResourceCrn(payload.getResourceCrn())
                .withResourceId(payload.getResourceId())
                .withResourceName(payload.getResourceName())
                .withProxyConfigCrn(payload.getProxyConfigCrn())
                .withPreviousProxyConfigCrn(payload.getPreviousProxyConfigCrn())
                .build();
        sendEvent(context, selector, event);
    }
}
