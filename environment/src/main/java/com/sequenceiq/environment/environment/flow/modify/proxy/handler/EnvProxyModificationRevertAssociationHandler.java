package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class EnvProxyModificationRevertAssociationHandler extends EventSenderAwareHandler<EnvProxyModificationFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvProxyModificationRevertAssociationHandler.class);

    private final ProxyConfigService proxyConfigService;

    private final EnvironmentService environmentService;

    public EnvProxyModificationRevertAssociationHandler(EventSender eventSender, ProxyConfigService proxyConfigService, EnvironmentService environmentService) {
        super(eventSender);
        this.proxyConfigService = proxyConfigService;
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return EnvProxyModificationHandlerSelectors.REVERT_PROXY_ASSOCIATION_HANDLER_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvProxyModificationFailedEvent> event) {
        EnvProxyModificationFailedEvent eventData = event.getData();
        LOGGER.info("Reverting proxy config to {} for env {}", eventData.getPreviousProxyConfigCrn(), eventData.getResourceCrn());

        try {
            ProxyConfig previousProxyConfig = eventData.getPreviousProxyConfigCrn() != null
                ? proxyConfigService.getByCrn(eventData.getPreviousProxyConfigCrn())
                : null;
            environmentService.updateProxyConfig(eventData.getResourceId(), previousProxyConfig);
        } catch (Exception e) {
            LOGGER.warn("Failed to revert proxy config", e);
        }

        EnvProxyModificationDefaultEvent envProxyModificationEvent = EnvProxyModificationDefaultEvent.builder()
                .withSelector(EnvProxyModificationStateSelectors.HANDLE_FAILED_MODIFY_PROXY_EVENT.selector())
                .withResourceCrn(eventData.getResourceCrn())
                .withResourceId(eventData.getResourceId())
                .withResourceName(eventData.getResourceName())
                .withProxyConfigCrn(eventData.getProxyConfigCrn())
                .withPreviousProxyConfigCrn(eventData.getPreviousProxyConfigCrn())
                .build();
        eventSender().sendEvent(envProxyModificationEvent, event.getHeaders());
    }
}
