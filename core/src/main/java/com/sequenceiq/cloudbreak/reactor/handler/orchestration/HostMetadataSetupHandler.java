package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.stack.flow.HostMetadataSetup;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class HostMetadataSetupHandler implements ReactorEventHandler<HostMetadataSetupRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private HostMetadataSetup hostMetadataSetup;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(HostMetadataSetupRequest.class);
    }

    @Override
    public void accept(Event<HostMetadataSetupRequest> event) {
        StackEvent request = event.getData();
        Selectable response;
        try {
            hostMetadataSetup.setupHostMetadata(request.getStackId());
            response = new HostMetadataSetupSuccess(request.getStackId());
        } catch (Exception e) {
            response = new HostMetadataSetupFailed(request.getStackId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
