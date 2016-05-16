package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailurePayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.OrchestrationEvent;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class BootstrapMachineHandler implements ReactorEventHandler<StackPayload> {
    @Inject
    private EventBus eventBus;
    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Override
    public String selector() {
        return OrchestrationEvent.BOOTSTRAP_MACHINES_REQUEST.name();
    }

    @Override
    public void accept(Event<StackPayload> event) {
        StackPayload request = event.getData();
        Selectable response;
        try {
            clusterBootstrapper.bootstrapMachines(request.getStackId());
            response = new StackPayload(OrchestrationEvent.BOOTSTRAP_MACHINES_DONE.name(), request.getStackId());
        } catch (Exception e) {
            response = new StackFailurePayload(OrchestrationEvent.BOOTSTRAP_MACHINES_FAILED.name(), request.getStackId(), e);
        }
        eventBus.notify(response.selector(), new Event(event.getHeaders(), response));
    }
}
