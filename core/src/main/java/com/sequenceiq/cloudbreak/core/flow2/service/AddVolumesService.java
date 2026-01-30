package com.sequenceiq.cloudbreak.core.flow2.service;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudConnectResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeStatus;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.VerticalScalingValidatorService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AddVolumesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesService.class);

    private static final Set<String> BLACKLISTED_ROLES = ImmutableSet.of("DATANODE", "ZEPPELIN_SERVER", "KAFKA_BROKER",
            "SCHEMA_REGISTRY_SERVER", "STREAMS_MESSAGING_MANAGER_SERVER", "SERVER", "NIFI_NODE", "NAMENODE", "STATESTORE",
            "CATALOGSERVER", "KUDU_MASTER", "KUDU_TSERVER", "SOLR_SERVER", "NIFI_REGISTRY_SERVER", "HUE_LOAD_BALANCER", "KNOX_GATEWAY", "GATEWAY");

    private static final Map<String, ResourceType> CLOUD_RESOURCE_TYPE_CONSTANTS = Map.of(CloudPlatform.AZURE.name(), ResourceType.AZURE_VOLUMESET,
            CloudPlatform.AWS.name(), ResourceType.AWS_VOLUMESET);

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private CloudResourceToResourceConverter cloudResourceToResourceConverter;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private InstanceGroupService instanceGroupService;

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

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ResourceService resourceService;

    @Inject
    private DeleteVolumesService deleteVolumesService;

    @Inject
    private VerticalScalingValidatorService verticalScalingValidatorService;

    @Inject
    private EntitlementService entitlementService;

    public Map<String, Map<String, String>> redeployStatesAndMountDisks(Stack stack, String requestGroup) throws Exception {
        String blueprintText = stack.getBlueprintJsonText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
        stopClouderaManagerServices(stack.getId(), requestGroup, hostTemplateServiceComponents);
        LOGGER.info("RE-Bootstrap machines");
        clusterBootstrapper.reBootstrapMachines(stack.getId());
        StackDto stackDto = stackDtoService.getById(stack.getId());
        clusterHostServiceRunner.updateClusterConfigs(stackDto, true);
        Map<String, Map<String, String>> fstabInformation = formatAndMountAfterAddingDisks(stack, requestGroup);
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
        boolean xfsSupport = entitlementService.isXfsForEphemeralDisksSupported(Crn.safeFromString(stack.getResourceCrn()).getAccountId());
        hostOrchestrator.updateMountDiskPillar(stack, gatewayConfigs, nodesWithDiskData, exitCriteriaModel,
                stack.getPlatformVariant(), xfsSupport);
        return hostOrchestrator.formatAndMountDisksAfterModifyingVolumesOnNodes(gatewayConfigs,
                nodesWithDiskData, allNodes, exitCriteriaModel);
    }

    private void stopClouderaManagerServices(Long stackId, String requestGroup, Set<ServiceComponent> hostTemplateServiceComponents) {
        try {
            StackDto stack = stackDtoService.getById(stackId);
            Set<String> hostTemplateComponents = hostTemplateServiceComponents.stream().map(ServiceComponent::getComponent).collect(Collectors.toSet());
            boolean restartServices = true;
            for (String service : hostTemplateComponents) {
                if (BLACKLISTED_ROLES.contains(service)) {
                    restartServices = false;
                }
            }
            if (restartServices) {
                configUpdateUtilService.stopClouderaManagerServices(stack, hostTemplateServiceComponents);
            }
        } catch (Exception ex) {
            LOGGER.warn("Exception while trying to stop cloudera manager services for group: {}", requestGroup);
            throw new CloudbreakServiceException(ex.getMessage());
        }
    }

    public void validateVolumeAddition(Long stackId, String instanceGroupName, AddVolumesValidateEvent payload) {
        Stack stack = stackService.getByIdWithLists(stackId);
        CloudConnectResources cloudConnectResources = cloudConnectorHelper.getCloudConnectorResources(stack);
        CloudConnector cloudConnector = cloudConnectResources.getCloudConnector();

        ValidationResult validationResult = verticalScalingValidatorService.validateAddVolumesRequest(stack, payload);
        if (validationResult.hasError()) {
            throw new CloudbreakServiceException(validationResult.getFormattedErrors());
        }
        CloudStack cloudStack = cloudConnectResources.getCloudStack();
        AuthenticatedContext ac = cloudConnectResources.getAuthenticatedContext();
        InstanceGroup instanceGroup = instanceGroupService.getInstanceGroupWithTemplateAndInstancesByGroupNameInStack(stackId, instanceGroupName)
                .orElseThrow(() -> new NotFoundException("Instance group with name " + instanceGroupName + " not found in stack " + stackId));
        List<String> instanceIds = instanceGroup.getNotDeletedInstanceMetaDataSet().stream().map(InstanceMetaData::getInstanceId).toList();

        DiskTypes diskTypes = cloudConnector.parameters().diskTypes();

        Integer expectedVolumeCount = instanceGroup.getTemplate().getVolumeTemplates()
                .stream()
                .filter(e -> {
                    VolumeParameterType volumeParameterType = diskTypes.diskMapping().get(e.getVolumeType());
                    if (volumeParameterType != null && volumeParameterType.equals(VolumeParameterType.EPHEMERAL)) {
                        // filtering out ephemeral volumes
                        return false;
                    }
                    return true;
                })
                .map(VolumeTemplate::getVolumeCount)
                .reduce(Integer::sum).orElse(0);
        Map<String, Integer> actualVolumeCounts = cloudConnector.volumeConnector().getAttachedVolumeCountPerInstance(ac, cloudStack, instanceIds);

        if (anyInstanceHasDifferentAttachedVolumeCount(actualVolumeCounts, expectedVolumeCount)) {
            String errorMessage = String.format("Expected %d volumes per instance in group '%s', but found different counts on the provider: %s",
                    expectedVolumeCount, instanceGroupName, actualVolumeCounts);
            LOGGER.error(errorMessage);
            throw new CloudbreakServiceException(errorMessage);
        }
    }

    private boolean anyInstanceHasDifferentAttachedVolumeCount(Map<String, Integer> actualVolumeCounts, Integer expectedVolumeCount) {
        return actualVolumeCounts.values()
                .stream()
                .anyMatch(Predicate.not(expectedVolumeCount::equals));
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
            connector.volumeConnector().attachVolumes(ac, cloudResources, cloudConnectResources.getCloudStack());
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

    public void updateResourceVolumeStatus(Set<Resource> resources, CloudVolumeStatus volumeStatus) {
        resources.forEach(res -> {
            Optional<VolumeSetAttributes> volumeSetOptional = resourceAttributeUtil.getTypedAttributes(res, VolumeSetAttributes.class);
            if (volumeSetOptional.isPresent()) {
                VolumeSetAttributes volumeSet = volumeSetOptional.get();
                List<VolumeSetAttributes.Volume> volumes = volumeSet.getVolumes();
                for (VolumeSetAttributes.Volume volume : volumes) {
                    volume.setCloudVolumeStatus(volumeStatus);
                }
            }
        });
        resourceService.saveAll(resources);
    }

    public void rollbackCreatedVolumes(Set<Resource> resources, Long stackId) throws Exception {
        Map<String, List<VolumeSetAttributes.Volume>> createdVolumesByInstancesMap = new HashMap<>();
        resources.forEach(res -> {
            Optional<VolumeSetAttributes> volumeSetOptional = resourceAttributeUtil.getTypedAttributes(res, VolumeSetAttributes.class);
            if (volumeSetOptional.isPresent()) {
                VolumeSetAttributes volumeSet = volumeSetOptional.get();
                List<VolumeSetAttributes.Volume> volumes = volumeSet.getVolumes();
                List<VolumeSetAttributes.Volume> volumeList = new ArrayList<>();
                List<VolumeSetAttributes.Volume> deleteVolumeList = new ArrayList<>();
                for (VolumeSetAttributes.Volume volume : volumes) {
                    if (null != volume.getCloudVolumeStatus() && (CloudVolumeStatus.CREATED.equals(volume.getCloudVolumeStatus())
                            || CloudVolumeStatus.REQUESTED.equals(volume.getCloudVolumeStatus()))) {
                        deleteVolumeList.add(volume);
                    } else {
                        volumeList.add(volume);
                    }
                }
                if (!deleteVolumeList.isEmpty()) {
                    volumeSet.setVolumes(volumeList);
                    setAttributes(res, volumeSet);
                    createdVolumesByInstancesMap.put(res.getInstanceId(), deleteVolumeList);
                }
            }
        });
        detachAndDeleteCreatedVolumes(stackId, createdVolumesByInstancesMap);
        resourceService.saveAll(resources);
    }

    private static void setAttributes(Resource res, VolumeSetAttributes volumeSet) {
        try {
            Json attributesJson = new Json(volumeSet);
            res.setAttributes(attributesJson);
        } catch (IllegalArgumentException e) {
            LOGGER.info("Failed to parse resource attributes. Attributes: [{}]", volumeSet, e);
            throw new IllegalStateException("Cannot parse stored resource attributes");
        }
    }

    private void detachAndDeleteCreatedVolumes(Long stackId, Map<String, List<VolumeSetAttributes.Volume>> createdVolumesByInstancesMap) throws Exception {
        StackDto stack = stackDtoService.getById(stackId);
        CloudConnectResources cloudConnectResources = cloudConnectorHelper.getCloudConnectorResources(stack);
        AuthenticatedContext ac = cloudConnectResources.getAuthenticatedContext();
        List<CloudResource> cloudResourcesToBeDeleted = stack.getResources().stream().filter(resource -> null != resource.getInstanceGroup()
                        && null != resource.getInstanceId()
                        && createdVolumesByInstancesMap.containsKey(resource.getInstanceId())
                        && null != CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform())
                        && CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform()).equals(resource.getResourceType()))
                .map(resource -> {
                    VolumeSetAttributes attributes = resourceAttributeUtil.getTypedAttributes(resource, VolumeSetAttributes.class).get();
                    attributes.setVolumes(createdVolumesByInstancesMap.get(resource.getInstanceId()));
                    setAttributes(resource, attributes);
                    return resource;
                })
                .map(s -> resourceToCloudResourceConverter.convert(s)).collect(toList());
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(stack.getCloudPlatform()),
                Variant.variant(stack.getPlatformVariant()));
        LOGGER.debug("Detaching cloud volume resources that were orphaned: {}.", createdVolumesByInstancesMap);
        deleteVolumesService.detachResources(cloudResourcesToBeDeleted, cloudPlatformVariant, ac);
        LOGGER.debug("Deleting cloud volume resources that were orphaned: {}.", createdVolumesByInstancesMap);
        deleteVolumesService.deleteResources(cloudResourcesToBeDeleted, cloudPlatformVariant, ac);
    }
}