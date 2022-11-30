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

@Component("ProxyConfigModificationFreeipaStateAction")
public class ProxyConfigModificationFreeipaStateAction extends AbstractEnvProxyModificationAction<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigModificationFreeipaStateAction.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    protected ProxyConfigModificationFreeipaStateAction(EnvironmentStatusUpdateService environmentStatusUpdateService) {
        super(EnvProxyModificationDefaultEvent.class);
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Override
    protected void doExecute(EnvProxyModificationContext context, EnvProxyModificationDefaultEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Env proxy modification freeipa step started");

        EnvironmentDto environmentDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS, ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_ON_FREEIPA_STARTED,
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_START_STATE);

        String selector = EnvProxyModificationHandlerSelectors.TRACK_FREEIPA_PROXY_MODIFICATION_EVENT.selector();
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
        return EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_FAILED;
    }
}
