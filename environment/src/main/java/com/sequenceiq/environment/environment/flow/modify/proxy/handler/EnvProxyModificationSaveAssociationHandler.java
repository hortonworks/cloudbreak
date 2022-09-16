package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

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

            EnvProxyModificationDefaultEvent envProxyModificationEvent = new EnvProxyModificationDefaultEvent(
                    EnvProxyModificationStateSelectors.FINISH_MODIFY_PROXY_EVENT.selector(), environmentDto, eventData.getProxyConfig());
            eventSender().sendEvent(envProxyModificationEvent, event.getHeaders());
        } catch (Exception e) {
            // we can not reach this without knowing that an environment exists with the given id, therefore the orElseThrow() should be safe
            EnvironmentDto environmentDto = environmentService.findById(eventData.getEnvironmentDto().getId()).orElseThrow();
            EnvProxyModificationFailedEvent envProxyModificationFailedEvent = new EnvProxyModificationFailedEvent(
                    environmentDto, EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED, e);
            eventSender().sendEvent(envProxyModificationFailedEvent, event.getHeaders());
        }
    }
}
