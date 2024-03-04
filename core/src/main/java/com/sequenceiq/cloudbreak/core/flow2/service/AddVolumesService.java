package com.sequenceiq.cloudbreak.core.flow2.service;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.CloudConnectResources;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class AddVolumesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesService.class);

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private CloudResourceToResourceConverter cloudResourceToResourceConverter;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ConfigUpdateUtilService configUpdateUtilService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private StackService stackService;

    @Inject
    private CloudConnectorHelper cloudConnectorHelper;

    public Map<String, Map<String, String>> redeployStatesAndMountDisks(Stack stack, String requestGroup) throws Exception {
        String blueprintText = stack.getBlueprint().getBlueprintJsonText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
        InMemoryStateStore.putStack(stack.getId(), PollGroup.POLLABLE);
        stopClouderaManagerServices(stack.getId(), requestGroup, hostTemplateServiceComponents);
        LOGGER.info("RE-Bootstrap machines");
        clusterBootstrapper.reBootstrapMachines(stack.getId());
        Map<String, Map<String, String>> fstabInformation = formatAndMountAfterAddingDisks(stack, requestGroup);
        StackDto stackDto = stackDtoService.getById(stack.getId());
        clusterHostServiceRunner.updateClusterConfigs(stackDto, true);
        InMemoryStateStore.deleteStack(stack.getId());
        LOGGER.info("Successfully mounted additional block storages for stack {} and group {}", stack.getId(), requestGroup);
        return fstabInformation;
    }

    private Map<String, Map<String, String>> formatAndMountAfterAddingDisks(Stack stack, String requestGroup) throws CloudbreakOrchestratorFailedException {
        Set<Node> allNodes = stackUtil.collectNodes(stack).stream().filter(node -> node.getHostGroup().equals(requestGroup)).collect(Collectors.toSet());
        Set<Node> nodesWithDiskData = stackUtil.collectNodesWithDiskData(stack).stream().filter(node -> node.getHostGroup().equals(requestGroup))
                .collect(Collectors.toSet());
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId());
        LOGGER.debug("Formatting and mounting disks.");
        Map<String, Map<String, String>> fstabInformation = hostOrchestrator.formatAndMountDisksAfterModifyingVolumesOnNodes(gatewayConfigs,
                nodesWithDiskData, allNodes, exitCriteriaModel);
        return fstabInformation;
    }

    private void stopClouderaManagerServices(Long stackId, String requestGroup, Set<ServiceComponent> hostTemplateServiceComponents) {
        try {
            StackDto stack = stackDtoService.getById(stackId);
            configUpdateUtilService.stopClouderaManagerServices(stack, hostTemplateServiceComponents);
        } catch (Exception ex) {
            LOGGER.warn("Exception while trying to stop cloudera manager services for group: {}", requestGroup);
            throw new CloudbreakServiceException(ex.getMessage());
        }
    }

    public List<Resource> createVolumes(Set<Resource> resources, VolumeSetAttributes.Volume volume, int volToAddPerInstance,
            String instanceGroup, Long stackId) throws CloudbreakServiceException {
        try {
            StackDto stack = stackDtoService.getById(stackId);
            List<CloudResource> cloudResources = resources.stream().map(resource -> resourceToCloudResourceConverter.convert(resource))
                    .collect(Collectors.toList());
            CloudConnectResources cloudConnectResources = cloudConnectorHelper.getCloudConnectorResources(stack);
            CloudStack cloudStack = cloudConnectResources.getCloudStack();
            CloudConnector connector = cloudConnectResources.getCloudConnector();
            AuthenticatedContext ac = cloudConnectResources.getAuthenticatedContext();
            LOGGER.debug("Calling cloud connector to create and attach {} volumes of type {} on stack name: {}.", volToAddPerInstance, volume.getType(),
                    stack.getName());
            Optional<Group> optionalGroup = cloudStack.getGroups().stream().filter(group -> group.getName().equals(instanceGroup)).findFirst();
            Group group = optionalGroup.orElseThrow();
            List<CloudResource> response = connector.volumeConnector().createVolumes(ac, group, volume, cloudStack, volToAddPerInstance,
                    cloudResources);
            LOGGER.debug("Response from creating disks on stack {}: response: {}", stack.getName(), response);
            List<Resource> newResources = response.stream().map(resource -> cloudResourceToResourceConverter.convert(resource)).toList();
            return populateResourceWithFields(newResources, resources, stack);
        } catch (Exception ex) {
            LOGGER.warn("Exception while creating volumes on stack id: {}, because: {}", stackId, ex.getMessage());
            throw new CloudbreakServiceException(ex.getMessage());
        }
    }

    public void attachVolumes(Set<Resource> resources, Long stackId) throws CloudbreakServiceException {
        try {
            StackDto stack = stackDtoService.getById(stackId);
            List<CloudResource> cloudResources = resources.stream().map(resource -> resourceToCloudResourceConverter.convert(resource))
                    .collect(Collectors.toList());
            CloudConnectResources cloudConnectResources = cloudConnectorHelper.getCloudConnectorResources(stack);
            CloudConnector connector = cloudConnectResources.getCloudConnector();
            AuthenticatedContext ac = cloudConnectResources.getAuthenticatedContext();
            LOGGER.debug("Calling cloud connector to attach new volumes created on stack name: {}.", stack.getName());
            connector.volumeConnector().attachVolumes(ac, cloudResources);
            LOGGER.debug("Finished attaching disks on stack {}", stack.getName());
        } catch (Exception ex) {
            LOGGER.warn("Exception while creating volumes on stack id: {}, because: {}", stackId, ex.getMessage());
            throw new CloudbreakServiceException(ex.getMessage());
        }
    }

    private List<Resource> populateResourceWithFields(List<Resource> newResources, Set<Resource> oldResources, StackDto stackDto) {
        Stack stack = stackService.getById(stackDto.getId());
        return newResources.stream().peek(resource -> {
            if (resource.getInstanceGroup() != null && resource.getInstanceId() != null) {
                Optional<Resource> optionalResource = oldResources.stream().filter(res -> resource.getInstanceGroup() != null
                        && resource.getInstanceId() != null && res.getInstanceId().equals(resource.getInstanceId())).findFirst();
                if (optionalResource.isPresent()) {
                    Resource oldResource = optionalResource.get();
                    resource.setId(oldResource.getId());
                }
                resource.setStack(stack);
            }
        }).toList();
    }
}
