package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class BootstrapMachineHandler implements ReactorEventHandler<BootstrapMachinesRequest> {
    @Inject
    private EventBus eventBus;
    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(BootstrapMachinesRequest.class);
    }

    @Override
    public void accept(Event<BootstrapMachinesRequest> event) {
        BootstrapMachinesRequest request = event.getData();
        Selectable response;
        try {
            clusterBootstrapper.bootstrapMachines(request.getStackId());
            response = new BootstrapMachinesSuccess(request.getStackId());
        } catch (Exception e) {
            response = new BootstrapMachinesFailed(request.getStackId(), e);
        }
        eventBus.notify(response.selector(), new Event(event.getHeaders(), response));
    }
}
