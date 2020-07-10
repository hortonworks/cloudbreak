package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapMachineHandler.class);

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
                LOGGER.info("RE-Bootstrap machines");
                clusterBootstrapper.reBootstrapMachines(request.getResourceId());
            } else {
                LOGGER.info("Bootstrap machines");
                clusterBootstrapper.bootstrapMachines(request.getResourceId());
            }
            response = new BootstrapMachinesSuccess(request.getResourceId());
        } catch (Exception e) {
            response = new BootstrapMachinesFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
