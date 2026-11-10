package com.sequenceiq.cloudbreak.reactor;

import static java.util.function.Predicate.not;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
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
        Set<String> hostNamesToStop = getHostsNotDeletedOnProvider(stopCmServicesOnHostsRequest.getHostNamesToStop(), stackDto);
        try {
            if (hostNamesToStop.isEmpty()) {
                LOGGER.info("No hosts remain to stop roles on after excluding hosts deleted on the provider.");
            } else {
                LOGGER.info("Stop roles on hosts: {}", hostNamesToStop);
                clusterDecomissionService.stopRolesOnHosts(hostNamesToStop, isStopServicesGracefully(stopCmServicesOnHostsRequest, stackDto));
            }
        } catch (CloudbreakException e) {
            LOGGER.error("Stop roles failed on hosts: {}", hostNamesToStop, e);
            return new DecommissionResult("Stop roles failed on hosts: " + e.getMessage(), e, event.getData());
        }
        return new DecommissionResult(stopCmServicesOnHostsRequest, stopCmServicesOnHostsRequest.getHostNamesToStop());
    }

    private Set<String> getHostsNotDeletedOnProvider(Set<String> requestedHostNames, StackDto stackDto) {
        Set<String> deletedHostNames = stackDto.getAllAvailableAndProviderDeletedInstances().stream()
                .filter(InstanceMetadataView::isDeletedOnProvider)
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(Objects::nonNull)
                .filter(requestedHostNames::contains)
                .collect(Collectors.toSet());
        if (!deletedHostNames.isEmpty()) {
            LOGGER.info("Skipping stopping roles on hosts already deleted on the provider: {}", deletedHostNames);
        }
        return requestedHostNames.stream()
                .filter(not(deletedHostNames::contains))
                .collect(Collectors.toSet());
    }

    private boolean isStopServicesGracefully(StopCmServicesOnHostsRequest stopCmServicesOnHostsRequest, StackDto stackDto) {
        return stopCmServicesOnHostsRequest.getHostNamesToStop().size() == 1 && distroXUpgradeService.isGracefulStopServicesNeeded(stackDto);
    }
}
