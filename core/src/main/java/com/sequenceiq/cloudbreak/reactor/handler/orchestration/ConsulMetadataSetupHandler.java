package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailurePayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.OrchestrationEvent;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulMetadataSetup;

import reactor.bus.Event;
import reactor.bus.EventBus;

public class ConsulMetadataSetupHandler implements ReactorEventHandler<StackPayload> {
    @Inject
    private EventBus eventBus;
    @Inject
    private ConsulMetadataSetup consulMetadataSetup;

    @Override
    public String selector() {
        return OrchestrationEvent.CONSUL_METADATA_SETUP_REQUEST.name();
    }

    @Override
    public void accept(Event<StackPayload> event) {
        StackPayload request = event.getData();
        Selectable response;
        try {
            consulMetadataSetup.setupConsulMetadata(request.getStackId());
            response = new StackPayload(OrchestrationEvent.CONSUL_METADATA_SETUP_DONE.name(), request.getStackId());
        } catch (Exception e) {
            response = new StackFailurePayload(OrchestrationEvent.CONSUL_METADATA_SETUP_FAILED.name(), request.getStackId(), e);
        }
        eventBus.notify(response.selector(), new Event(event.getHeaders(), response));
    }
}
