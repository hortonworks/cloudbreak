package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_BOOTSTRAPPER_ERROR_BOOTSTRAP_FAILED_ON_NODES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_BOOTSTRAPPER_ERROR_DELETING_NODE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_BOOTSTRAPPER_ERROR_INVALID_NODECOUNT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component
public class ClusterBootstrapperErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBootstrapperErrorHandler.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private Clock clock;

    public void terminateFailedNodes(HostOrchestrator hostOrchestrator, ContainerOrchestrator containerOrchestrator,
            Stack stack, GatewayConfig gatewayConfig, Set<Node> nodes)
            throws CloudbreakOrchestratorFailedException {
        List<String> allAvailableNode;
        allAvailableNode = hostOrchestrator != null ? hostOrchestrator.getAvailableNodes(gatewayConfig, nodes)
                : containerOrchestrator.getAvailableNodes(gatewayConfig, nodes);
        List<Node> missingNodes = selectMissingNodes(nodes, allAvailableNode);
        if (!missingNodes.isEmpty()) {
            eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), CLUSTER_BOOTSTRAPPER_ERROR_BOOTSTRAP_FAILED_ON_NODES,
                    Arrays.asList(String.valueOf(missingNodes.size())));

            for (Node missingNode : missingNodes) {
                InstanceMetaData instanceMetaData =
                        instanceMetaDataService.findNotTerminatedByPrivateAddress(stack.getId(), missingNode.getPrivateIp())
                                .orElseThrow(NotFoundException.notFound("instanceMetaData", missingNode.getPrivateIp()));
                InstanceGroup ig = instanceGroupService.findOneWithInstanceMetadataByGroupNameInStack(stack.getId(),
                        instanceMetaData.getInstanceGroup().getGroupName())
                        .orElseThrow(NotFoundException.notFound("instanceGroup", instanceMetaData.getInstanceGroup().getGroupName()));
                if (ig.getNodeCount() < 1) {
                    throw new CloudbreakOrchestratorFailedException(cloudbreakMessagesService.getMessage(
                            CLUSTER_BOOTSTRAPPER_ERROR_INVALID_NODECOUNT.getMessage(),
                            Collections.singletonList(ig.getGroupName())));
                }
                instanceGroupService.save(ig);
                eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), CLUSTER_BOOTSTRAPPER_ERROR_DELETING_NODE,
                        Arrays.asList(instanceMetaData.getInstanceId(), ig.getGroupName()));
                deleteResourceAndDependencies(stack, instanceMetaData);
                deleteInstanceResourceFromDatabase(stack, instanceMetaData);
                instanceMetaData.setTerminationDate(clock.getCurrentTimeMillis());
                instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                instanceMetaDataService.save(instanceMetaData);
                LOGGER.debug("InstanceMetadata [name: {}, id: {}] status set to {}.", instanceMetaData.getId(), instanceMetaData.getInstanceId(),
                        instanceMetaData.getInstanceStatus());
            }
        }
    }

    private List<Node> selectMissingNodes(Iterable<Node> clusterNodes, Iterable<String> availableNodes) {
        List<Node> missingNodes = new ArrayList<>();
        for (Node node : clusterNodes) {
            boolean contains = false;
            for (String nodeAddress : availableNodes) {
                if (nodeAddress.equals(node.getPrivateIp())) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                missingNodes.add(node);
            }
        }
        return missingNodes;
    }

    private void deleteResourceAndDependencies(Stack stack, InstanceMetaData instanceMetaData) {
        LOGGER.debug("Rolling back instance [name: {}, id: {}]", instanceMetaData.getId(), instanceMetaData.getInstanceId());
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add(instanceMetaData.getInstanceId());
        connector.removeInstances(stack, instanceIds, instanceMetaData.getInstanceGroup().getGroupName());
        LOGGER.debug("Deleted instance [name: {}, id: {}]", instanceMetaData.getId(), instanceMetaData.getInstanceId());
    }

    private void deleteInstanceResourceFromDatabase(Stack stack, InstanceMetaData instanceMetaData) {
        resourceService.findByStackIdAndNameAndType(stack.getId(), instanceMetaData.getInstanceId(), null)
                .ifPresent(value -> resourceService.delete(value));
    }

}
