package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;

@Component("ProxyConfigModificationDatalakeStateAction")
public class ProxyConfigModificationDatalakeStateAction extends AbstractEnvProxyModificationAction<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigModificationDatalakeStateAction.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    protected ProxyConfigModificationDatalakeStateAction(EnvironmentStatusUpdateService environmentStatusUpdateService) {
        super(EnvProxyModificationDefaultEvent.class);
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Override
    protected void doExecute(EnvProxyModificationContext context, EnvProxyModificationDefaultEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Env proxy modification datalake step started");

        EnvironmentDto environmentDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATALAKE_IN_PROGRESS, ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_ON_DATALAKE_STARTED,
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_DATALAKE_STATE);

        String selector = EnvProxyModificationHandlerSelectors.TRACK_DATALAKE_PROXY_MODIFICATION_EVENT.selector();
        EnvironmentEvent event = EnvProxyModificationDefaultEvent.builder()
                .withSelector(selector)
                .withEnvironmentDto(environmentDto)
                .withProxyConfig(payload.getProxyConfig())
                .withPreviousProxyConfig(context.getPreviousProxyConfig())
                .build();
        sendEvent(context, selector, event);
    }

    @Override
    protected EnvironmentStatus getFailureEnvironmentStatus() {
        return EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATALAKE_FAILED;
    }
}
