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
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;

@Component("ProxyConfigModificationDatahubsStateAction")
public class ProxyConfigModificationDatahubsStateAction extends AbstractEnvProxyModificationAction<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigModificationDatahubsStateAction.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    protected ProxyConfigModificationDatahubsStateAction(EnvironmentStatusUpdateService environmentStatusUpdateService) {
        super(EnvProxyModificationDefaultEvent.class);
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Override
    protected void doExecute(EnvProxyModificationContext context, EnvProxyModificationDefaultEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Env proxy modification datahubs step started");

        environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATAHUBS_IN_PROGRESS, ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_ON_DATAHUBS_STARTED,
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_DATAHUBS_STATE);

        String selector = EnvProxyModificationHandlerSelectors.TRACK_DATAHUBS_PROXY_MODIFICATION_EVENT.selector();
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

    @Override
    protected EnvironmentStatus getFailureEnvironmentStatus() {
        return EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATAHUBS_FAILED;
    }
}
