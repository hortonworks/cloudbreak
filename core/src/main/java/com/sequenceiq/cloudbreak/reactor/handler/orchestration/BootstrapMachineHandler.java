package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class BootstrapMachineHandler implements EventHandler<BootstrapMachinesRequest> {
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
            if (request.isReBootstrap()) {
                clusterBootstrapper.reBootstrapMachines(request.getResourceId());
            } else {
                clusterBootstrapper.bootstrapMachines(request.getResourceId());
            }
            response = new BootstrapMachinesSuccess(request.getResourceId());
        } catch (Exception e) {
            response = new BootstrapMachinesFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
