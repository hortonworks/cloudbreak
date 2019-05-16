package com.sequenceiq.freeipa.flow.freeipa.provision.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.FreeIpaInstallService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class InstallFreeIpaServicesHandler implements EventHandler<InstallFreeIpaServicesRequest> {
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
            response = new InstallFreeIpaServicesFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
