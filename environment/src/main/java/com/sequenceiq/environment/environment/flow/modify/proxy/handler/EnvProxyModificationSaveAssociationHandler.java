package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class EnvProxyModificationSaveAssociationHandler extends EventSenderAwareHandler<EnvProxyModificationDefaultEvent> {

    private final EnvironmentService environmentService;

    public EnvProxyModificationSaveAssociationHandler(EventSender eventSender, EnvironmentService environmentService) {
        super(eventSender);
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return EnvProxyModificationHandlerSelectors.SAVE_NEW_PROXY_ASSOCIATION_HANDLER_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvProxyModificationDefaultEvent> event) {
        EnvProxyModificationDefaultEvent eventData = event.getData();
        try {
            EnvironmentDto environmentDto = environmentService.updateProxyConfig(eventData.getEnvironmentDto().getId(), eventData.getProxyConfig());

            EnvProxyModificationDefaultEvent envProxyModificationEvent = EnvProxyModificationDefaultEvent.builder()
                    .withSelector(EnvProxyModificationStateSelectors.MODIFY_PROXY_FREEIPA_EVENT.selector())
                    .withEnvironmentDto(environmentDto)
                    .withProxyConfig(eventData.getProxyConfig())
                    .withPreviousProxyConfig(eventData.getPreviousProxyConfig())
                    .build();
            eventSender().sendEvent(envProxyModificationEvent, event.getHeaders());
        } catch (Exception e) {
            EnvProxyModificationFailedEvent envProxyModificationFailedEvent = EnvProxyModificationFailedEvent.builder()
                    .withEnvironmentDto(eventData.getEnvironmentDto())
                    .withProxyConfig(eventData.getProxyConfig())
                    .withPreviousProxyConfig(eventData.getPreviousProxyConfig())
                    .withEnvironmentStatus(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED)
                    .withException(e)
                    .build();
            eventSender().sendEvent(envProxyModificationFailedEvent, event.getHeaders());
        }
    }
}
