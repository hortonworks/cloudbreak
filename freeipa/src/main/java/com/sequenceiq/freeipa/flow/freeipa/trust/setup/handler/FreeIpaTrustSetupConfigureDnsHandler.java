package com.sequenceiq.freeipa.flow.freeipa.trust.setup.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupConfigureDnsFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupConfigureDnsRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupConfigureDnsSuccess;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.ConfigureDnsServerService;

@Component
public class FreeIpaTrustSetupConfigureDnsHandler extends ExceptionCatcherEventHandler<FreeIpaTrustSetupConfigureDnsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustSetupConfigureDnsHandler.class);

    @Inject
    private ConfigureDnsServerService configureDnsServerService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaTrustSetupConfigureDnsRequest> event) {
        return new FreeIpaTrustSetupConfigureDnsFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaTrustSetupConfigureDnsRequest> event) {
        FreeIpaTrustSetupConfigureDnsRequest request = event.getData();
        try {
            configureDnsServerService.configureDnsServer(request.getResourceId());
            return new FreeIpaTrustSetupConfigureDnsSuccess(request.getResourceId());
        } catch (Exception e) {
            return new FreeIpaTrustSetupConfigureDnsFailed(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaTrustSetupConfigureDnsRequest.class);
    }
}
