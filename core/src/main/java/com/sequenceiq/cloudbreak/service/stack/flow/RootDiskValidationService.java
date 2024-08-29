package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType.ROOT_DISK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.RootVolumeFetchDto;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.util.CloudConnectResources;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class RootDiskValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootDiskValidationService.class);

    private static final Map<String, ResourceType> PLATFORM_RESOURCE_TYPE_MAP = ImmutableMap.of(CloudPlatform.AWS.name(), ResourceType.AWS_ROOT_DISK,
            CloudPlatform.AZURE.name(), ResourceType.AZURE_DISK);

    @Inject
    private ResourceService resourceService;

    @Inject
    private CloudConnectorHelper cloudConnectorHelper;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private CloudResourceToResourceConverter cloudResourceToResourceConverter;

    @Inject
    private TemplateService templateService;

    public List<Resource> fetchRootDiskResourcesForGroup(StackDto stack, DiskUpdateRequest updateRequest) throws Exception {
        String platform = stack.getCloudPlatform();
        List<Resource> resources = new ArrayList<>();
        String groupName = updateRequest.getGroup();
        if (PLATFORM_RESOURCE_TYPE_MAP.containsKey(platform)) {
            CloudConnectResources cloudConnectResources = cloudConnectorHelper.getCloudConnectorResources(stack);
            CloudStack cloudStack = cloudConnectResources.getCloudStack();
            CloudConnector connector = cloudConnectResources.getCloudConnector();
            AuthenticatedContext ac = cloudConnectResources.getAuthenticatedContext();
            Group group = cloudStack.getGroups().stream().filter(grp -> grp.getName().equals(groupName)).findFirst()
                    .orElseThrow();
            List<String> instanceIdsInGroup = group.getInstances().stream().map(CloudInstance::getInstanceId).toList();
            resources.addAll(resourceService.findByStackIdAndType(stack.getId(), PLATFORM_RESOURCE_TYPE_MAP.get(platform)).stream()
                    .filter(res -> instanceIdsInGroup.contains(res.getInstanceId())).toList());
            LOGGER.debug("Resources from database for root disk: {}", resources);
            if (resources.isEmpty()) {
                List<CloudResource> instancesAsCloudResource = getInstancesAsCloudResourceForAzure(group, platform, stack.getId());
                String resourceGroupName = cloudStack.getParameters().getOrDefault("resourceGroupName", "");
                RootVolumeFetchDto rootVolumeFetchDto = new RootVolumeFetchDto(ac, group, resourceGroupName, instancesAsCloudResource);
                List<CloudResource> cloudResources = connector.volumeConnector().getRootVolumes(rootVolumeFetchDto);
                LOGGER.debug("Fetched cloud resource from cloud provider for root disk: {}", cloudResources);
                resources.addAll(cloudResources.stream().map(res -> cloudResourceToResourceConverter.convert(res)).toList());
            }
            checkUpdateRequired(resources.getFirst(), updateRequest);
            LOGGER.debug("Deleting Root Disk Resources in database as new resources will be added when instances get created - Resources: {}", resources);
            resourceService.deleteAll(resources);
            LOGGER.debug("Deleted Root Disk Resources in database - Resources: {}", resources);
            updateStackTemplate(stack, updateRequest);
        }
        return resources;
    }

    private void checkUpdateRequired(Resource resource, DiskUpdateRequest diskUpdateRequest) throws BadRequestException {
        try {
            VolumeSetAttributes volumeSetAttributes = resource.getAttributes().get(VolumeSetAttributes.class);
            VolumeSetAttributes.Volume rootDisk = volumeSetAttributes.getVolumes().getFirst();
            if (rootDisk.getSize() == diskUpdateRequest.getSize() && rootDisk.getType().equalsIgnoreCase(diskUpdateRequest.getVolumeType())) {
                throw new BadRequestException("No update required.");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception e) {
            LOGGER.warn("Exception while trying to parse VolumeSetAttributes from resource: ", e);
        }
    }

    private void updateStackTemplate(StackDto stackDto, DiskUpdateRequest updateRequest) {
        LOGGER.debug("Updating template with root update request: {}", updateRequest);
        InstanceGroupDto instanceGroupDto = stackDto.getInstanceGroupByInstanceGroupName(updateRequest.getGroup());
        Template template = instanceGroupDto.getInstanceGroup().getTemplate();
        if (updateRequest.getDiskType().equals(ROOT_DISK)) {
            if (updateRequest.getSize() > 0) {
                template.setRootVolumeSize(updateRequest.getSize());
            }
        }
        templateService.savePure(template);
        LOGGER.debug("Updated template after save: {}", template);
    }

    private List<CloudResource> getInstancesAsCloudResourceForAzure(Group group, String platform, Long stackId) {
        List<CloudResource> cloudResources = new ArrayList<>();
        if ("AZURE".equalsIgnoreCase(platform)) {
            List<String> instanceIdsInGroup = group.getInstances().stream().map(ins -> ins.getInstanceId()).toList();
            List<Resource> resources = resourceService.findByStackIdAndType(stackId, AZURE_INSTANCE).stream()
                    .filter(res -> instanceIdsInGroup.contains(res.getInstanceId())).toList();
            cloudResources.addAll(resources.stream().map(res -> resourceToCloudResourceConverter.convert(res)).toList());
        }
        return cloudResources;
    }
}
