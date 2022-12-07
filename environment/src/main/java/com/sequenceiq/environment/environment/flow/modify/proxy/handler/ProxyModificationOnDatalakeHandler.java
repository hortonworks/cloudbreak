package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.sdx.SdxPollerService;
import com.sequenceiq.environment.proxy.domain.ProxyConfigBase;
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
        EnvironmentDto environmentDto = eventData.getEnvironmentDto();
        String previousProxyCrn = Optional.ofNullable(eventData.getPreviousProxyConfig())
                .map(ProxyConfigBase::getResourceCrn)
                .orElse(null);
        try {
            LOGGER.info("Starting and waiting for datalake modify proxy config");
            sdxPollerService.modifyProxyConfigOnAttachedDatalakeClusters(environmentDto.getId(), environmentDto.getName(), previousProxyCrn);

            EnvProxyModificationDefaultEvent envProxyModificationEvent = EnvProxyModificationDefaultEvent.builder()
                    .withSelector(EnvProxyModificationStateSelectors.FINISH_MODIFY_PROXY_EVENT.selector())
                    .withEnvironmentDto(environmentDto)
                    .withProxyConfig(eventData.getProxyConfig())
                    .withPreviousProxyConfig(eventData.getPreviousProxyConfig())
                    .build();
            eventSender().sendEvent(envProxyModificationEvent, event.getHeaders());
        } catch (Exception e) {
            EnvProxyModificationFailedEvent envProxyModificationFailedEvent = EnvProxyModificationFailedEvent.builder()
                    .withEnvironmentDto(environmentDto)
                    .withProxyConfig(eventData.getProxyConfig())
                    .withPreviousProxyConfig(eventData.getPreviousProxyConfig())
                    .withEnvironmentStatus(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATALAKE_FAILED)
                    .withException(e)
                    .build();
            eventSender().sendEvent(envProxyModificationFailedEvent, event.getHeaders());
        }
    }
}
