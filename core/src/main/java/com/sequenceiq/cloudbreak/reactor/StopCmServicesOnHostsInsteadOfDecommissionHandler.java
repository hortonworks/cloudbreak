package com.sequenceiq.cloudbreak.reactor;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.StopCmServicesOnHostsRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StopCmServicesOnHostsInsteadOfDecommissionHandler extends ExceptionCatcherEventHandler<StopCmServicesOnHostsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopCmServicesOnHostsInsteadOfDecommissionHandler.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DistroXUpgradeService distroXUpgradeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopCmServicesOnHostsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StopCmServicesOnHostsRequest> event) {
        return new DecommissionResult("Unexpected error: " + e.getMessage(), e, event.getData());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StopCmServicesOnHostsRequest> event) {
        StopCmServicesOnHostsRequest stopCmServicesOnHostsRequest = event.getData();
        StackDto stackDto = stackDtoService.getById(stopCmServicesOnHostsRequest.getResourceId());
        ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stackDto).clusterDecomissionService();
        try {
            LOGGER.info("Stop roles on hosts: {}", stopCmServicesOnHostsRequest.getHostNamesToStop());
            clusterDecomissionService.stopRolesOnHosts(stopCmServicesOnHostsRequest.getHostNamesToStop(),
                    isStopServicesGracefully(stopCmServicesOnHostsRequest, stackDto));
        } catch (CloudbreakException e) {
            LOGGER.error("Stop roles failed on hosts: {}", stopCmServicesOnHostsRequest.getHostNamesToStop(), e);
            return new DecommissionResult("Stop roles failed on hosts: " + e.getMessage(), e, event.getData());
        }
        return new DecommissionResult(stopCmServicesOnHostsRequest, stopCmServicesOnHostsRequest.getHostNamesToStop());
    }

    private boolean isStopServicesGracefully(StopCmServicesOnHostsRequest stopCmServicesOnHostsRequest, StackDto stackDto) {
        return stopCmServicesOnHostsRequest.getHostNamesToStop().size() == 1 && distroXUpgradeService.isGracefulStopServicesNeeded(stackDto);
    }
}
