package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ConsulMetadataSetupFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ConsulMetadataSetupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ConsulMetadataSetupSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulMetadataSetup;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ConsulMetadataSetupHandler implements ReactorEventHandler<ConsulMetadataSetupRequest> {
    @Inject
    private EventBus eventBus;
    @Inject
    private ConsulMetadataSetup consulMetadataSetup;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ConsulMetadataSetupRequest.class);
    }

    @Override
    public void accept(Event<ConsulMetadataSetupRequest> event) {
        StackEvent request = event.getData();
        Selectable response;
        try {
            consulMetadataSetup.setupConsulMetadata(request.getStackId());
            response = new ConsulMetadataSetupSuccess(request.getStackId());
        } catch (Exception e) {
            response = new ConsulMetadataSetupFailed(request.getStackId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
