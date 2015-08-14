package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisioningService;
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

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudPlatformResolver platformResolver;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private TlsSecurityService tlsSecurityService;

    private enum Msg {

        BOOTSTRAPPER_ERROR_BOOTSTRAP_FAILED_ON_NODES("bootstrapper.error.nodes.failed"),
        BOOTSTRAPPER_ERROR_DELETING_NODE("bootstrapper.error.deleting.node"),
        BOOTSTRAPPER_ERROR_INVALID_NODECOUNT("bootstrapper.error.invalide.nodecount");


        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    public void terminateFailedNodes(ContainerOrchestrator orchestrator, Stack stack, GatewayConfig gatewayConfig, Set<Node> nodes)
            throws CloudbreakOrchestratorFailedException {
        List<String> allAvailableNode = orchestrator.getAvailableNodes(gatewayConfig, nodes);
        List<Node> missingNodes = selectMissingNodes(nodes, allAvailableNode);
        if (missingNodes.size() > 0) {
            String message = cloudbreakMessagesService.getMessage(Msg.BOOTSTRAPPER_ERROR_BOOTSTRAP_FAILED_ON_NODES.code(), Arrays.asList(missingNodes.size()));
            LOGGER.info(message);
            eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);

            for (Node missingNode : missingNodes) {
                InstanceMetaData instanceMetaData =
                        instanceMetaDataRepository.findNotTerminatedByPrivateAddress(stack.getId(), missingNode.getPrivateIp());
                InstanceGroup ig = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), instanceMetaData.getInstanceGroup().getGroupName());
                ig.setNodeCount(ig.getNodeCount() - 1);
                if (ig.getNodeCount() < 1) {
                    throw new CloudbreakOrchestratorFailedException(cloudbreakMessagesService.getMessage(Msg.BOOTSTRAPPER_ERROR_INVALID_NODECOUNT.code(),
                            Arrays.asList(ig.getGroupName())));
                }
                instanceGroupRepository.save(ig);
                message = cloudbreakMessagesService.getMessage(Msg.BOOTSTRAPPER_ERROR_DELETING_NODE.code(),
                        Arrays.asList(instanceMetaData.getInstanceId(), ig.getGroupName()));
                LOGGER.info(message);
                eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);
                deleteResourceAndDependencies(stack, instanceMetaData);
                deleteInstanceResourceFromDatabase(stack, instanceMetaData);
                instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                instanceMetaDataRepository.save(instanceMetaData);
                LOGGER.info("InstanceMetadata [name: {}, id: {}] status set to {}.", instanceMetaData.getId(), instanceMetaData.getInstanceId(),
                        instanceMetaData.getInstanceStatus());
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
        LOGGER.info("Rolling back instance [name: {}, id: {}]", instanceMetaData.getId(), instanceMetaData.getInstanceId());
        CloudPlatformConnector cloudPlatformConnector = platformResolver.connector(stack.cloudPlatform());
        Set<String> instanceIds = new HashSet<>();
        instanceIds.add(instanceMetaData.getInstanceId());
        Map<InstanceGroupType, String> userdata = buildUserData(stack, stack.cloudPlatform(), cloudPlatformConnector);
        cloudPlatformConnector.removeInstances(stack, userdata.get(InstanceGroupType.GATEWAY),
                userdata.get(InstanceGroupType.CORE), instanceIds, instanceMetaData.getInstanceGroup().getGroupName());
        LOGGER.info("Deleted instance [name: {}, id: {}]", instanceMetaData.getId(), instanceMetaData.getInstanceId());
    }

    private Map<InstanceGroupType, String> buildUserData(Stack stack, CloudPlatform platform, CloudPlatformConnector connector) {
        try {
            return userDataBuilder.buildUserData(platform,
                    tlsSecurityService.readPublicSshKey(stack.getId()), connector.getSSHUser(singletonMap(ProvisioningService.PLATFORM, platform.name())));
        } catch (CloudbreakSecuritySetupException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void deleteInstanceResourceFromDatabase(Stack stack, InstanceMetaData instanceMetaData) {
        MetadataSetup metadataSetup = platformResolver.metadata(stack.cloudPlatform());
        ResourceType instanceResourceType = metadataSetup.getInstanceResourceType();
        Resource resource = resourceRepository.findByStackIdAndNameAndType(stack.getId(), instanceMetaData.getInstanceId(), instanceResourceType);
        if (resource != null) {
            resourceRepository.delete(resource.getId());
        }
    }
}
