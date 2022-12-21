package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
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
public class EnvProxyModificationSaveAssociationHandler extends EventSenderAwareHandler<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvProxyModificationSaveAssociationHandler.class);

    private final ProxyConfigService proxyConfigService;

    private final EnvironmentService environmentService;

    public EnvProxyModificationSaveAssociationHandler(EventSender eventSender, ProxyConfigService proxyConfigService, EnvironmentService environmentService) {
        super(eventSender);
        this.proxyConfigService = proxyConfigService;
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
            LOGGER.info("Saving proxy config {} for env {}", eventData.getProxyConfigCrn(), eventData.getResourceCrn());
            ProxyConfig proxyConfig = eventData.getProxyConfigCrn() != null
                    ? proxyConfigService.getByCrn(eventData.getProxyConfigCrn())
                    : null;
            environmentService.updateProxyConfig(eventData.getResourceId(), proxyConfig);

            EnvProxyModificationDefaultEvent envProxyModificationEvent = EnvProxyModificationDefaultEvent.builder()
                    .withSelector(EnvProxyModificationStateSelectors.MODIFY_PROXY_FREEIPA_EVENT.selector())
                    .withResourceCrn(eventData.getResourceCrn())
                    .withResourceId(eventData.getResourceId())
                    .withResourceName(eventData.getResourceName())
                    .withProxyConfigCrn(eventData.getProxyConfigCrn())
                    .withPreviousProxyConfigCrn(eventData.getPreviousProxyConfigCrn())
                    .build();
            eventSender().sendEvent(envProxyModificationEvent, event.getHeaders());
        } catch (Exception e) {
            eventSender().sendEvent(new EnvProxyModificationFailedEvent(eventData, e, EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED), event.getHeaders());
        }
    }
}
