package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.ConfigureDnsFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.ConfigureDnsRequest;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.ConfigureDnsSuccess;
import com.sequenceiq.freeipa.service.freeipa.crossrealm.ConfigureDnsServerService;

@Component
public class ConfigureDnsHandler extends ExceptionCatcherEventHandler<ConfigureDnsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureDnsHandler.class);

    @Inject
    private ConfigureDnsServerService configureDnsServerService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ConfigureDnsRequest> event) {
        return new ConfigureDnsFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ConfigureDnsRequest> event) {
        ConfigureDnsRequest request = event.getData();
        try {
            LOGGER.info("ipa dnsforwardzone-add");
            LOGGER.info("ipa dnsrecord-add");
            LOGGER.info("Validate network connectivity (dig dig dig)");
            LOGGER.info("Address SSSD bug: service sssd stop; rm -f /var/lib/sss/db/*; service sssd start");
            configureDnsServerService.configureDnsServer(request.getResourceId());
            return new ConfigureDnsSuccess(request.getResourceId());
        } catch (Exception e) {
            return new ConfigureDnsFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ConfigureDnsRequest.class);
    }
}
