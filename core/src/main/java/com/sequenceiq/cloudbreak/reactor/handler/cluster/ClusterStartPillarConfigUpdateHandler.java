package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateFailed;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateResult;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterStartPillarConfigUpdateHandler extends ExceptionCatcherEventHandler<ClusterStartPillarConfigUpdateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartPillarConfigUpdateHandler.class);

    @Inject
    private PillarConfigUpdateService pillarConfigUpdateService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CloudbreakFlowMessageService cloudbreakFlowMessageService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStartPillarConfigUpdateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterStartPillarConfigUpdateRequest> event) {
        return new PillarConfigUpdateFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterStartPillarConfigUpdateRequest> event) {
        ClusterStartPillarConfigUpdateRequest request = event.getData();
        Selectable response;
        try {
            pillarConfigUpdateService.doConfigUpdate(request.getResourceId());
            response = new ClusterStartPillarConfigUpdateResult(request);
        } catch (Exception e) {
            LOGGER.warn("Pillar configuration update failed.", e);
            if (ExceptionUtils.getRootCause(e) instanceof CloudbreakOrchestratorFailedException orchestratorFailedException &&
                    !orchestratorFailedException.getNodesWithErrors().isEmpty()) {
                if (isPrimaryGatewayFailed(request.getResourceId(), orchestratorFailedException)) {
                    response = new PillarConfigUpdateFailed(request.getResourceId(), e);
                } else {
                    updateFailedInstances(request.getResourceId(), orchestratorFailedException);
                    sendNotification(request.getResourceId(), orchestratorFailedException);
                    response = new ClusterStartPillarConfigUpdateResult(request);
                }
            } else {
                response = new PillarConfigUpdateFailed(request.getResourceId(), e);
            }
        }
        return response;
    }

    private boolean isPrimaryGatewayFailed(Long stackId, CloudbreakOrchestratorFailedException orchestratorFailedException) {
        try {
            return instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stackId)
                    .map(instance -> orchestratorFailedException.getNodesWithErrors().containsKey(instance.getDiscoveryFQDN()))
                    .orElse(false);
        } catch (Exception e) {
            LOGGER.warn("Couldn't check if primary gateway is failed.", e);
            return false;
        }
    }

    private void updateFailedInstances(Long stackId, CloudbreakOrchestratorFailedException orchestratorFailedException) {
        try {
            Map<String, InstanceMetadataView> instances = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stackId)
                    .stream()
                    .filter(instance -> instance.getDiscoveryFQDN() != null)
                    .collect(Collectors.toMap(InstanceMetadataView::getDiscoveryFQDN, instance -> instance));
            List<Long> failedInstanceIds = new ArrayList<>();
            orchestratorFailedException.getNodesWithErrors()
                    .keySet()
                    .forEach(hostname -> {
                        if (instances.containsKey(hostname)) {
                            failedInstanceIds.add(instances.get(hostname).getId());
                        }
                    });
            instanceMetaDataService.updateInstanceStatuses(failedInstanceIds, InstanceStatus.SERVICES_UNHEALTHY, "Pillar configuration update failed.");
        } catch (Exception e) {
            LOGGER.warn("Couldn't update instances to failed state.", e);
        }
    }

    private void sendNotification(Long stackId, CloudbreakOrchestratorFailedException orchestratorFailedException) {
        try {
            String reason = orchestratorFailedException.getNodesWithErrors().asMap()
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        if (CollectionUtils.isEmpty(entry.getValue())) {
                            return entry.getKey();
                        } else {
                            return entry.getKey() + " - " + String.join(" ", entry.getValue());
                        }
                    })
                    .collect(Collectors.joining("\n"));
            cloudbreakFlowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), ResourceEvent.CLUSTER_START_INSTANCES_FAILED, reason);
        } catch (Exception e) {
            LOGGER.warn("Couldn't send notification about failed instances.", e);
        }
    }
}
