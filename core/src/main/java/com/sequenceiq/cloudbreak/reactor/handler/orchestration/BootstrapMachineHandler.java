package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class BootstrapMachineHandler extends ExceptionCatcherEventHandler<BootstrapMachinesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapMachineHandler.class);

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(BootstrapMachinesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<BootstrapMachinesRequest> event) {
        return new BootstrapMachinesFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<BootstrapMachinesRequest> event) {
        BootstrapMachinesRequest request = event.getData();
        Selectable response;
        try {
            if (request.isReBootstrap()) {
                LOGGER.info("RE-Bootstrap machines");
                clusterBootstrapper.reBootstrapMachines(request.getResourceId());
                clusterProxyService.reRegisterCluster(request.getResourceId());
            } else {
                LOGGER.info("Bootstrap machines");
                clusterBootstrapper.bootstrapMachines(request.getResourceId());
            }
            response = new BootstrapMachinesSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.warn("Bootstrap machines failed (rebootstrap:{})", request.isReBootstrap(), e);
            response = new BootstrapMachinesFailed(request.getResourceId(), e);
        }
        return response;
    }
}
