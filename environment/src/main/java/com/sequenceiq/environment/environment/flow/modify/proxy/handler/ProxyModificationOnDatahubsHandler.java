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
import com.sequenceiq.environment.environment.service.datahub.DatahubModifyProxyConfigPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class ProxyModificationOnDatahubsHandler extends EventSenderAwareHandler<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyModificationOnDatahubsHandler.class);

    private final DatahubModifyProxyConfigPollerService datahubModifyProxyConfigPollerService;

    protected ProxyModificationOnDatahubsHandler(EventSender eventSender, DatahubModifyProxyConfigPollerService datahubModifyProxyConfigPollerService) {
        super(eventSender);
        this.datahubModifyProxyConfigPollerService = datahubModifyProxyConfigPollerService;
    }

    @Override
    public String selector() {
        return EnvProxyModificationHandlerSelectors.TRACK_DATAHUBS_PROXY_MODIFICATION_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvProxyModificationDefaultEvent> event) {
        EnvProxyModificationDefaultEvent eventData = event.getData();
        try {
            LOGGER.info("Starting and waiting for all datahubs modify proxy config");
            datahubModifyProxyConfigPollerService.modifyProxyOnAttachedDatahubs(
                    eventData.getResourceId(), eventData.getResourceCrn(), eventData.getPreviousProxyConfigCrn());

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
                    new EnvProxyModificationFailedEvent(eventData, e, EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATAHUBS_FAILED), event.getHeaders());
        }
    }
}
