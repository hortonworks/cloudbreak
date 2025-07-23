package com.sequenceiq.cloudbreak.service.datalake;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_TRIGGER_EVENT;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeRequest;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class DiskUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskUpdateService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private TemplateService templateService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private EntitlementService entitlementService;

    public boolean isDiskTypeChangeSupported(String platform) {
        return cloudParameterCache.isDiskTypeChangeSupported(platform);
    }

    public void updateDiskTypeAndSize(String group, String volumeType, int size, List<Volume> volumesToUpdate, Long stackId) throws Exception {
        StackDto stackDto = stackDtoService.getById(stackId);
        validateDiskUpdateRequest(volumeType, size, CloudPlatform.valueOf(stackDto.getCloudPlatform()), stackDto.getResourceCrn());
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(stackDto.getCloudPlatform()),
                Variant.variant(stackDto.getPlatformVariant()));
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudPlatformVariant);
        AuthenticatedContext ac = getAuthenticatedContext(cloudConnector, stackDto);
        List<String> volumeIds = volumesToUpdate.stream().map(Volume::getId).toList();
        cloudConnector.volumeConnector().updateDiskVolumes(ac, volumeIds, volumeType, size);
        for (Resource resource : stackDto.getDiskResources()) {
            Optional<VolumeSetAttributes> optionalVolumeSetAttributes = resourceAttributeUtil.getTypedAttributes(resource, VolumeSetAttributes.class);
            if (group.equals(resource.getInstanceGroup()) && optionalVolumeSetAttributes.isPresent()) {
                VolumeSetAttributes volumeSetAttributes = optionalVolumeSetAttributes.get();
                List<VolumeSetAttributes.Volume> volumes = volumeSetAttributes.getVolumes();
                for (VolumeSetAttributes.Volume volume : volumes) {
                    if (volumeIds.contains(volume.getId())) {
                        updateVolumeTypeAndSize(volumeType, size, volume);
                    }
                }
                volumeSetAttributes.setVolumes(volumes);
                resourceAttributeUtil.setTypedAttributes(resource, volumeSetAttributes);
            }
        }
        LOGGER.info("Updated resources for disk update flow::{}", stackDto.getDiskResources());
        resourceService.saveAll(stackDto.getDiskResources());
        updateTemplate(stackId, group, volumeType, size);
    }

    private void updateVolumeTypeAndSize(String volumeType, int size, VolumeSetAttributes.Volume volume) {
        if (size > 0) {
            volume.setSize(size);
        }
        if (null != volumeType) {
            volume.setType(volumeType);
        }
    }

    public void stopCMServices(long stackId) throws Exception {
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        clusterApi.clusterModificationService().stopCluster(true);
    }

    public void startCMServices(long stackId) throws Exception {
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        clusterApi.clusterModificationService().startCluster();
    }

    public FlowIdentifier resizeDisks(long stackId, String instanceGroup, String volumeType, int size, List<Volume> volumesToUpdate) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Stack Resize Flow triggered for stack {}", stack.getName());
        DiskResizeRequest diskResizeRequest = DiskResizeRequest.Builder.builder()
                .withSelector(DISK_RESIZE_TRIGGER_EVENT.selector())
                .withStackId(stackId)
                .withInstanceGroup(instanceGroup)
                .withSize(size)
                .withVolumeType(volumeType)
                .withVolumesToUpdate(volumesToUpdate)
                .build();
        FlowIdentifier flowIdentifier = reactorNotifier.notify(stackId, diskResizeRequest.selector(), diskResizeRequest);
        LOGGER.info("DiskResizeRequest event is triggered for stack {}", stack.getName());
        return flowIdentifier;
    }

    private AuthenticatedContext getAuthenticatedContext(CloudConnector cloudConnector, StackDto stack) {
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .build();
        return cloudConnector.authentication().authenticate(cloudContext, cloudCredential);
    }

    private void updateTemplate(Long stackId, String group, String volumeType, int size) {
        LOGGER.debug("Updating stack template and saving it to CBDB.");
        Optional<InstanceGroupView> optionalGroup = instanceGroupService
                .findInstanceGroupViewByStackIdAndGroupName(stackId, group);

        DiskTypes diskTypes = getDiskTypes(stackDtoService.getById(stackId));
        if (optionalGroup.isPresent()) {
            InstanceGroupView instanceGroup = optionalGroup.get();
            Template template = instanceGroup.getTemplate();
            for (VolumeTemplate volumeTemplateInTheDatabase : notEphemeralVolumes(template, diskTypes)) {
                if (null != group && StringUtils.isNotBlank(volumeType)) {
                    volumeTemplateInTheDatabase.setVolumeType(volumeType);
                }
                if (size > 0) {
                    volumeTemplateInTheDatabase.setVolumeSize(size);
                }
            }
            templateService.savePure(template);
        }
    }

    private List<VolumeTemplate> notEphemeralVolumes(Template template, DiskTypes diskTypes) {
        return template.getVolumeTemplates()
                .stream()
                .filter(e -> !isEphemeral(e, diskTypes))
                .toList();
    }

    public void resizeDisksAndUpdateFstab(Stack stack, String instanceGroup) throws CloudbreakOrchestratorFailedException {
        ResourceType diskResourceType = stack.getDiskResourceType();
        Long stackId = stack.getId();
        LOGGER.debug("Collecting resources based on stack id {} and resource type {} filtered by instance group {}.", stackId, diskResourceType,
                instanceGroup);
        List<Resource> resourceList = resourceService.findAllByStackIdAndInstanceGroupAndResourceTypeIn(stackId, instanceGroup,
                List.of(diskResourceType)).stream().filter(res -> null != res.getInstanceId()).toList();
        stack.setResources(new HashSet<>(resourceList));
        Set<Node> allNodesInTargetGroup = stackUtil.collectNodes(stack).stream().filter(node -> node.getHostGroup().equals(instanceGroup))
                .collect(Collectors.toSet());
        Cluster cluster = stack.getCluster();
        Set<Node> nodesWithDiskDataInTargetGroup = stackUtil.collectNodesWithDiskData(stack).stream().filter(node -> node.getHostGroup()
                .equals(instanceGroup)).collect(Collectors.toSet());
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
        LOGGER.debug("Calling host orchestrator for resizing and fetching fstab information for nodes - {}", allNodesInTargetGroup);
        Map<String, Map<String, String>> fstabInformation = hostOrchestrator.resizeDisksOnNodes(gatewayConfigs, nodesWithDiskDataInTargetGroup,
                allNodesInTargetGroup, exitCriteriaModel);

        parseFstabAndPersistDiskInformation(fstabInformation, stack);
    }

    private void parseFstabAndPersistDiskInformation(Map<String, Map<String, String>> fstabInformation, Stack stack) {
        LOGGER.debug("Parsing fstab information from host orchestrator resize disks - {}", fstabInformation);
        fstabInformation.forEach((hostname, value) -> {
            Optional<String> instanceIdOptional = stack.getInstanceMetaDataAsList().stream()
                    .filter(instanceMetaData -> hostname.equals(instanceMetaData.getDiscoveryFQDN()))
                    .map(InstanceMetaData::getInstanceId)
                    .findFirst();

            if (instanceIdOptional.isPresent()) {
                String uuids = value.getOrDefault("uuids", "");
                String fstab = value.getOrDefault("fstab", "");
                if (!StringUtils.isEmpty(uuids) && !StringUtils.isEmpty(fstab)) {
                    LOGGER.debug("Persisting resources for instance id - {}, hostname - {}, uuids - {}, fstab - {}.", instanceIdOptional.get(), hostname,
                            uuids, fstab);
                    persistUuidAndFstab(stack, instanceIdOptional.get(), hostname, uuids, fstab);
                }
            }
        });
    }

    private void persistUuidAndFstab(Stack stack, String instanceId, String discoveryFQDN, String uuids, String fstab) {
        resourceService.saveAll(stack.getDiskResources().stream()
                .filter(volumeSet -> instanceId.equals(volumeSet.getInstanceId()))
                .peek(volumeSet -> resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class).ifPresent(volumeSetAttributes -> {
                    volumeSetAttributes.setUuids(uuids);
                    volumeSetAttributes.setFstab(fstab);
                    if (!discoveryFQDN.equals(volumeSetAttributes.getDiscoveryFQDN())) {
                        LOGGER.info("DiscoveryFQDN is updated for {} to {}", volumeSet.getResourceName(), discoveryFQDN);
                    }
                    volumeSetAttributes.setDiscoveryFQDN(discoveryFQDN);
                    resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
                }))
                .collect(Collectors.toList()));
    }

    private void validateDiskUpdateRequest(String volumeType, int size, CloudPlatform cloudPlatform, String clusterCrn) {
        if (cloudPlatform == CloudPlatform.AZURE) {
            if (!entitlementService.azureResizeDiskEnabled(Crn.safeFromString(clusterCrn).getAccountId())) {
                throw new BadRequestException("Resizing Disk for Azure is not enabled for this account");
            } else if (StringUtils.isNotEmpty(volumeType)) {
                throw new BadRequestException("Changing Volume Type is not supported for Azure");
            }
        } else if (cloudPlatform == CloudPlatform.AWS) {
            if (StringUtils.isEmpty(volumeType) && size == 0) {
                throw new BadRequestException("Volume Type or Disk Size must be specified for AWS disk modification.");
            }
        }
    }

    public DiskTypes getDiskTypes(StackDto stackDto) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(stackDto.getCloudPlatform()),
                Variant.variant(stackDto.getPlatformVariant())
        );
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudPlatformVariant);
        return cloudConnector.parameters().diskTypes();
    }

    private boolean isEphemeral(VolumeTemplate volumeTemplate, DiskTypes diskTypes) {
        return VolumeParameterType.EPHEMERAL.equals(diskTypes.diskMapping().get(volumeTemplate.getVolumeType()));
    }

}