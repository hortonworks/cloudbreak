package com.sequenceiq.freeipa.flow.freeipa.provision.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaSuccess;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.FreeIpaPostInstallService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class PostInstallFreeIpaHandler implements EventHandler<PostInstallFreeIpaRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private FreeIpaPostInstallService freeIpaPostInstallService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PostInstallFreeIpaRequest.class);
    }

    @Override
    public void accept(Event<PostInstallFreeIpaRequest> event) {
        StackEvent request = event.getData();
        Selectable response;
        try {
            freeIpaPostInstallService.postInstallFreeIpa(request.getResourceId());
            response = new PostInstallFreeIpaSuccess(request.getResourceId());
        } catch (Exception e) {
            response = new PostInstallFreeIpaFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
