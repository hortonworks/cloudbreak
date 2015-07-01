package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

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
    private HostMetadataRepository hostMetadataRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Inject
    private CloudbreakEventService eventService;

    @javax.annotation.Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    public void terminateFailedNodes(ContainerOrchestrator orchestrator, Stack stack, GatewayConfig gatewayConfig, Set<Node> nodes)
            throws CloudbreakOrchestratorFailedException {
        List<String> allAvailableNode = orchestrator.getAvailableNodes(gatewayConfig, nodes);
        List<Node> missingNodes = selectMissingNodes(nodes, allAvailableNode);
        if (missingNodes.size() > 0) {
            String message = String.format("Bootstrap failed on %s nodes. These nodes will be terminated.", missingNodes.size());
            LOGGER.info(message);
            eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);
            for (Node missingNode : missingNodes) {
                InstanceMetaData instanceMetaData =
                        instanceMetaDataRepository.findNotTerminatedByPrivateAddress(stack.getId(), missingNode.getPrivateIp());
                InstanceGroup ig = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), instanceMetaData.getInstanceGroup().getGroupName());
                ig.setNodeCount(ig.getNodeCount() - 1);
                if (ig.getNodeCount() < 1) {
                    throw new CloudbreakOrchestratorFailedException(String.format("%s instancegroup nodecount was lower than 1 cluster creation failed.",
                            ig.getGroupName()));
                }
                instanceGroupRepository.save(ig);
                message = String.format("Delete '%s' node. and Decrease the nodecount on %s instancegroup",
                        instanceMetaData.getInstanceId(), ig.getGroupName());
                LOGGER.info(message);
                eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);
                deleteResourceAndDependencies(stack, instanceMetaData);
                deleteInstanceResourceFromDatabase(stack, instanceMetaData);
                instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                instanceMetaDataRepository.save(instanceMetaData);
                LOGGER.info(String.format("The status of instanceMetadata with %s id and %s name setted to TERMINATED.",
                        instanceMetaData.getId(), instanceMetaData.getInstanceId()));
            }
        }
    }

    private List<Node> selectMissingNodes(Set<Node> clusterNodes, List<String> availableNodes) {
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
        LOGGER.info(String.format("Instance %s rollback started.", instanceMetaData.getInstanceId()));
        CloudPlatformConnector cloudPlatformConnector = cloudPlatformConnectors.get(stack.cloudPlatform());
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add(instanceMetaData.getInstanceId());
        cloudPlatformConnector.removeInstances(stack, instanceIds, instanceMetaData.getInstanceGroup().getGroupName());
        LOGGER.info(String.format("Instance deleted with %s id and %s name.", instanceMetaData.getId(), instanceMetaData.getInstanceId()));
    }

    private void deleteInstanceResourceFromDatabase(Stack stack, InstanceMetaData instanceMetaData) {
        MetadataSetup metadataSetup = metadataSetups.get(stack.cloudPlatform());
        ResourceType instanceResourceType = metadataSetup.getInstanceResourceType();
        Resource resource = resourceRepository.findByStackIdAndNameAndType(stack.getId(), instanceMetaData.getInstanceId(), instanceResourceType);
        if (resource != null) {
            resourceRepository.delete(resource.getId());
        }
    }
}
