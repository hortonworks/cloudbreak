package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;

@Component
public class ClusterBootstrapperErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBootstrapperErrorHandler.class);

    @Inject
    private ResourceRepository resourceRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    private enum Msg {

        BOOTSTRAPPER_ERROR_BOOTSTRAP_FAILED_ON_NODES("bootstrapper.error.nodes.failed"),
        BOOTSTRAPPER_ERROR_DELETING_NODE("bootstrapper.error.deleting.node"),
        BOOTSTRAPPER_ERROR_INVALID_NODECOUNT("bootstrapper.error.invalide.nodecount");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    public void terminateFailedNodes(HostOrchestrator hostOrchestrator, ContainerOrchestrator containerOrchestrator,
            Stack stack, GatewayConfig gatewayConfig, Set<Node> nodes)
            throws CloudbreakOrchestratorFailedException {
        List<String> allAvailableNode;
        allAvailableNode = hostOrchestrator != null ? hostOrchestrator.getAvailableNodes(gatewayConfig, nodes)
                : containerOrchestrator.getAvailableNodes(gatewayConfig, nodes);
        List<Node> missingNodes = selectMissingNodes(nodes, allAvailableNode);
        if (!missingNodes.isEmpty()) {
            String message = cloudbreakMessagesService.getMessage(Msg.BOOTSTRAPPER_ERROR_BOOTSTRAP_FAILED_ON_NODES.code(),
                    Collections.singletonList(missingNodes.size()));
            LOGGER.debug(message);
            eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);

            for (Node missingNode : missingNodes) {
                InstanceMetaData instanceMetaData =
                        instanceMetaDataRepository.findNotTerminatedByPrivateAddress(stack.getId(), missingNode.getPrivateIp());
                InstanceGroup ig = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), instanceMetaData.getInstanceGroup().getGroupName());
                if (ig.getNodeCount() < 1) {
                    throw new CloudbreakOrchestratorFailedException(cloudbreakMessagesService.getMessage(Msg.BOOTSTRAPPER_ERROR_INVALID_NODECOUNT.code(),
                            Collections.singletonList(ig.getGroupName())));
                }
                instanceGroupRepository.save(ig);
                message = cloudbreakMessagesService.getMessage(Msg.BOOTSTRAPPER_ERROR_DELETING_NODE.code(),
                        Arrays.asList(instanceMetaData.getInstanceId(), ig.getGroupName()));
                LOGGER.debug(message);
                eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);
                deleteResourceAndDependencies(stack, instanceMetaData);
                deleteInstanceResourceFromDatabase(stack, instanceMetaData);
                long timeInMillis = Calendar.getInstance().getTimeInMillis();
                instanceMetaData.setTerminationDate(timeInMillis);
                instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                instanceMetaDataRepository.save(instanceMetaData);
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
        Resource resource = resourceRepository.findByStackIdAndNameAndType(stack.getId(), instanceMetaData.getInstanceId(), null);
        if (resource != null) {
            resourceRepository.delete(resource);
        }
    }
}
