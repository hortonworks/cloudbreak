package com.sequenceiq.freeipa.flow.freeipa.provision.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaInstallService;

@Component
public class InstallFreeIpaServicesHandler implements EventHandler<InstallFreeIpaServicesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstallFreeIpaServicesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private FreeIpaInstallService freeIpaInstallService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InstallFreeIpaServicesRequest.class);
    }

    @Override
    public void accept(Event<InstallFreeIpaServicesRequest> event) {
        StackEvent request = event.getData();
        Selectable response;
        try {
            freeIpaInstallService.installFreeIpa(request.getResourceId());
            response = new InstallFreeIpaServicesSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("FreeIPA service install failed", e);
            response = new InstallFreeIpaServicesFailed(request.getResourceId(), e, ERROR);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
