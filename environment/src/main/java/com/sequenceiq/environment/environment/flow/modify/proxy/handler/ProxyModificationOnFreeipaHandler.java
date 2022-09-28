package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class ProxyModificationOnFreeipaHandler extends EventSenderAwareHandler<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyModificationOnFreeipaHandler.class);

    private final FreeIpaPollerService freeIpaPollerService;

    protected ProxyModificationOnFreeipaHandler(EventSender eventSender, FreeIpaPollerService freeIpaPollerService) {
        super(eventSender);
        this.freeIpaPollerService = freeIpaPollerService;
    }

    @Override
    public String selector() {
        return EnvProxyModificationHandlerSelectors.TRACK_FREEIPA_PROXY_MODIFICATION_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvProxyModificationDefaultEvent> event) {
        EnvProxyModificationDefaultEvent eventData = event.getData();
        try {
            LOGGER.info("Starting and waiting for freeipa modify proxy config");
            freeIpaPollerService.waitForModifyProxyConfig(eventData.getResourceId(), eventData.getResourceCrn());

            EnvProxyModificationDefaultEvent envProxyModificationEvent = new EnvProxyModificationDefaultEvent(
                    EnvProxyModificationStateSelectors.FINISH_MODIFY_PROXY_EVENT.selector(), eventData.getEnvironmentDto(), eventData.getProxyConfig());
            eventSender().sendEvent(envProxyModificationEvent, event.getHeaders());
        } catch (Exception e) {
            EnvProxyModificationFailedEvent envProxyModificationFailedEvent = new EnvProxyModificationFailedEvent(
                    eventData.getEnvironmentDto(), eventData.getProxyConfig(), EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_FAILED, e);
            eventSender().sendEvent(envProxyModificationFailedEvent, event.getHeaders());
        }
    }
}
