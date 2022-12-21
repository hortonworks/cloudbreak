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
import com.sequenceiq.environment.environment.service.sdx.SdxPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class ProxyModificationOnDatalakeHandler extends EventSenderAwareHandler<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyModificationOnDatalakeHandler.class);

    private final SdxPollerService sdxPollerService;

    protected ProxyModificationOnDatalakeHandler(EventSender eventSender, SdxPollerService sdxPollerService) {
        super(eventSender);
        this.sdxPollerService = sdxPollerService;
    }

    @Override
    public String selector() {
        return EnvProxyModificationHandlerSelectors.TRACK_DATALAKE_PROXY_MODIFICATION_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvProxyModificationDefaultEvent> event) {
        EnvProxyModificationDefaultEvent eventData = event.getData();
        try {
            LOGGER.info("Starting and waiting for datalake modify proxy config");
            sdxPollerService.modifyProxyConfigOnAttachedDatalakeClusters(eventData.getResourceId(), eventData.getResourceName(),
                    eventData.getPreviousProxyConfigCrn());

            EnvProxyModificationDefaultEvent envProxyModificationEvent = EnvProxyModificationDefaultEvent.builder()
                    .withSelector(EnvProxyModificationStateSelectors.FINISH_MODIFY_PROXY_EVENT.selector())
                    .withResourceCrn(eventData.getResourceCrn())
                    .withResourceId(eventData.getResourceId())
                    .withResourceName(eventData.getResourceName())
                    .withProxyConfigCrn(eventData.getProxyConfigCrn())
                    .withPreviousProxyConfigCrn(eventData.getPreviousProxyConfigCrn())
                    .build();
            eventSender().sendEvent(envProxyModificationEvent, event.getHeaders());
        } catch (Exception e) {
            eventSender().sendEvent(
                    new EnvProxyModificationFailedEvent(eventData, e, EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATALAKE_FAILED), event.getHeaders());
        }
    }
}
