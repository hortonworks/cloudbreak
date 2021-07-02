package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.arm.AvailabilityZoneId;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.azure.service.AzureResourceNameService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AzureVolumeResourceBuilder extends AbstractAzureComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureVolumeResourceBuilder.class);

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Override
    public List<CloudResource> create(AzureContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.info("Creating volume resources");
        List<CloudResource> computeResources = context.getComputeResources(privateId);
        if (Objects.isNull(computeResources) || computeResources.isEmpty()) {
            return null;
        }
        CloudResource vm = context.getComputeResources(privateId)
                .stream()
                .filter(cloudResource -> ResourceType.AZURE_INSTANCE.equals(cloudResource.getType()))
                .findFirst()
                .get();

        Optional<CloudResource> reattachableVolumeSet = computeResources.stream()
                .filter(resource -> ResourceType.AZURE_VOLUMESET.equals(resource.getType()))
                .filter(cloudResource -> CommonStatus.DETACHED.equals(cloudResource.getStatus()) || vm.getInstanceId().equals(cloudResource.getInstanceId()))
                .findFirst();
        LOGGER.info("Reattachable volume set {}",
                reattachableVolumeSet.map(cloudResource -> "is present with name:" + cloudResource.getName())
                        .orElse("is not present"));

        return List.of(reattachableVolumeSet.orElseGet(createVolumeSet(privateId, auth, group, vm,
                context.getStringParameter(PlatformParametersConsts.RESOURCE_CRN_PARAMETER))));
    }

    private Supplier<CloudResource> createVolumeSet(long privateId, AuthenticatedContext auth, Group group, CloudResource vm, String stackCrn) {
        return () -> {
            AzureResourceNameService resourceNameService = getResourceNameService();

            InstanceTemplate template = group.getReferenceInstanceTemplate();
            String groupName = group.getName();
            CloudContext cloudContext = auth.getCloudContext();
            String stackName = cloudContext.getName();
            String availabilityZone = getAvailabilityZone(auth, vm);

            return new Builder()
                    .persistent(true)
                    .type(resourceType())
                    .name(resourceNameService.resourceName(resourceType(), stackName, groupName, privateId, stackCrn))
                    .group(group.getName())
                    .status(CommonStatus.REQUESTED)
                    .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                            .withAvailabilityZone(availabilityZone)
                            .withDeleteOnTermination(Boolean.TRUE)
                            .withVolumes(
                                    template.getVolumes().stream()
                                            .map(volume -> new VolumeSetAttributes.Volume(
                                                    resourceNameService.resourceName(ResourceType.AZURE_DISK, stackName, groupName, privateId,
                                                            template.getVolumes().indexOf(volume), stackCrn),
                                                    null, volume.getSize(), volume.getType(), volume.getVolumeUsageType()))
                                            .collect(toList()))
                            .build()))
                    .build();
        };
    }

    private String getAvailabilityZone(AuthenticatedContext auth, CloudResource vm) {
        LOGGER.debug("Fetching availability zone for vm {}", vm.getName());
        AzureClient client = getAzureClient(auth);
        String vmName = vm.getName();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(auth.getCloudContext(), vm);

        // Azure Java client returns a Set, but VM can only have at most 1 AZ
        return client.getAvailabilityZone(resourceGroupName, vmName).stream()
                .findFirst()
                .map(AvailabilityZoneId::toString)
                .orElse(null);
    }

    @Override
    public List<CloudResource> build(AzureContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        LOGGER.info("Create volumes on provider");
        AzureClient client = getAzureClient(auth);

        Map<String, List<VolumeSetAttributes.Volume>> volumeSetMap = Collections.synchronizedMap(new HashMap<>());

        List<Future<?>> futures = new ArrayList<>();
        List<CloudResource> requestedResources = buildableResource.stream()
                .filter(cloudResource -> CommonStatus.REQUESTED.equals(cloudResource.getStatus()))
                .collect(toList());
        CloudContext cloudContext = auth.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack);
        String region = cloudContext.getLocation().getRegion().getRegionName();
        String diskEncryptionSetId = getDiskEncryptionSetId(group);
        for (CloudResource resource : requestedResources) {
            volumeSetMap.put(resource.getName(), Collections.synchronizedList(new ArrayList<>()));
            VolumeSetAttributes volumeSet = getVolumeSetAttributes(resource);
            DeviceNameGenerator generator = new DeviceNameGenerator();
            futures.addAll(volumeSet.getVolumes().stream()
                    .map(volume -> intermediateBuilderExecutor.submit(() -> {
                        Disk result = client.getDiskByName(resourceGroupName, volume.getId());
                        if (result == null) {
                            result = client.createManagedDisk(
                                    volume.getId(), volume.getSize(), AzureDiskType.getByValue(
                                            volume.getType()), region, resourceGroupName, cloudStack.getTags(), diskEncryptionSetId);
                        } else {
                            LOGGER.info("Managed disk for resource group: {}, name: {} already exists: {}", resourceGroupName, volume.getId(), result);
                        }
                        String volumeId = result.id();
                        volumeSetMap.get(resource.getName()).add(new VolumeSetAttributes.Volume(volumeId, generator.next(), volume.getSize(), volume.getType(),
                                volume.getCloudVolumeUsageType()));
                    }))
                    .collect(toList()));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        return buildableResource.stream()
                .peek(resource -> {
                    List<VolumeSetAttributes.Volume> volumes = volumeSetMap.get(resource.getName());
                    if (!CollectionUtils.isEmpty(volumes)) {
                        getVolumeSetAttributes(resource).setVolumes(volumes);
                    }
                })
                .map(copyResourceWithNewStatus(CommonStatus.CREATED))
                .collect(toList());
    }

    private String getDiskEncryptionSetId(Group group) {
        CloudInstance cloudInstance = group.getReferenceInstanceConfiguration();
        AzureInstanceView azureInstanceView = AzureInstanceView.builder(cloudInstance).build();
        return azureInstanceView.isManagedDiskEncryptionWithCustomKeyEnabled() ? azureInstanceView.getDiskEncryptionSetId() : null;
    }

    private Function<CloudResource, CloudResource> copyResourceWithNewStatus(CommonStatus status) {
        return resource -> new Builder()
                .persistent(true)
                .group(resource.getGroup())
                .type(resource.getType())
                .status(status)
                .name(resource.getName())
                .params(resource.getParameters())
                .build();
    }

    @Override
    @Retryable(value = RuntimeException.class, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public CloudResource delete(AzureContext context, AuthenticatedContext auth, CloudResource resource) throws PreserveResourceException {
        LOGGER.info("Delete the disks from the instances if they are not reattached. {}", resource);
        VolumeSetAttributes volumeSetAttributes = getVolumeSetAttributes(resource);
        List<CloudResourceStatus> cloudResourceStatuses = checkResources(ResourceType.AZURE_VOLUMESET, context, auth, List.of(resource));
        boolean anyDeleted = cloudResourceStatuses.stream().map(CloudResourceStatus::getStatus).anyMatch(ResourceStatus.DELETED::equals);
        preserveVolumeIfDoNotDeleteOnTermination(auth, resource, volumeSetAttributes, anyDeleted);

        LOGGER.info("Resource {} will be deleted.", resource.getName());
        AzureClient client = getAzureClient(auth);
        List<CloudResourceStatus> removableDisks = cloudResourceStatuses
                .stream()
                .filter(cloudResourceStatus -> ResourceStatus.CREATED.equals(cloudResourceStatus.getStatus()) ||
                        (ResourceStatus.ATTACHED.equals(cloudResourceStatus.getStatus()) && volumeSetAttributes.getDeleteOnTermination()))
                .collect(toList());

        deleteVolumes(client, removableDisks);
        return null;
    }

    private void deleteVolumes(AzureClient client, List<CloudResourceStatus> removableDisks) {
        LOGGER.info("The following volumes are going to be deleted: [{}]",
                removableDisks.stream().map(CloudResourceStatus::toString).collect(Collectors.joining(",")));
        for (CloudResourceStatus cloudResourceStatus : removableDisks) {
            CloudResource cloudResource = cloudResourceStatus.getCloudResource();
            List<String> volumeIds = getVolumeIds(cloudResource);
            LOGGER.info("VolumeIds to delete: {}", String.join(", ", volumeIds));
            for (String volumeId : volumeIds) {
                detachVolume(client, cloudResource, volumeId);
            }
            LOGGER.info("Going to attempt to delete the following managed disks (based on the following IDs) on Azure: {}",
                    String.join(",", volumeIds));
            azureUtils.deleteManagedDisks(client, volumeIds);
        }
    }

    private List<String> getVolumeIds(CloudResource cloudResource) {
        return getVolumeSetAttributes(cloudResource).getVolumes().stream()
                .map(VolumeSetAttributes.Volume::getId)
                .collect(toList());
    }

    private void detachVolume(AzureClient client, CloudResource cloudResource, String volumeId) {
        String instanceName = cloudResource.getInstanceId();
        try {
            Disk diskById = client.getDiskById(volumeId);
            if (diskById.isAttachedToVirtualMachine()) {
                LOGGER.info("Trying to detach volume {} from {}", volumeId, instanceName);
                VirtualMachine virtualMachine = client.getVirtualMachine(diskById.virtualMachineId());
                client.detachDiskFromVm(volumeId, virtualMachine);
            } else {
                LOGGER.info("No need to detach disk(VolumeId:'{}') as it is not attached to any VM", volumeId);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Can not detach " + volumeId + " from " + instanceName, e);
        }
    }

    private void preserveVolumeIfDoNotDeleteOnTermination(AuthenticatedContext auth, CloudResource resource, VolumeSetAttributes volumeSetAttributes,
            boolean anyDeleted) throws PreserveResourceException {
        if (!volumeSetAttributes.getDeleteOnTermination() && !anyDeleted) {
            LOGGER.info("Resource {} will be preserved for later reattachment.", resource.getName());
            resource.setStatus(CommonStatus.DETACHED);
            volumeSetAttributes.setDeleteOnTermination(Boolean.TRUE);
            resource.putParameter(CloudResource.ATTRIBUTES, volumeSetAttributes);
            resourceNotifier.notifyUpdate(resource, auth.getCloudContext());
            LOGGER.debug("The following volume will be preserved: {}", resource);
            throw new PreserveResourceException("Resource will be preserved for later reattachment." + resource.getName());
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_VOLUMESET;
    }

    @Override
    protected List<CloudResourceStatus> checkResources(ResourceType type, AzureContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        AzureClient client = getAzureClient(auth);
        List<CloudResource> volumeResources = StreamSupport.stream(resources.spliterator(), false)
                .filter(r -> r.getType().equals(resourceType()))
                .collect(toList());
        CloudResource resourceGroup = context.getNetworkResources().stream()
                .filter(r -> r.getType().equals(ResourceType.AZURE_RESOURCE_GROUP))
                .findFirst()
                .orElseThrow(() -> new AzureResourceException("Resource group resource not found"));
        String resourceGroupName = resourceGroup.getName();

        PagedList<Disk> existingDisks = client.listDisksByResourceGroup(resourceGroupName);
        ResourceStatus volumeSetStatus = getResourceStatus(existingDisks, volumeResources);
        return volumeResources.stream()
                .map(resource -> new CloudResourceStatus(resource, volumeSetStatus))
                .collect(toList());
    }

    private ResourceStatus getResourceStatus(PagedList<Disk> existingDisks, List<CloudResource> volumeResources) {
        List<String> expectedIdList = volumeResources.stream()
                .map(this::getVolumeSetAttributes)
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .map(VolumeSetAttributes.Volume::getId)
                .collect(toList());
        List<String> actualIdList = existingDisks.stream()
                .map(Disk::id)
                .collect(toList());
        if (!actualIdList.containsAll(expectedIdList)) {
            LOGGER.debug("Resource status is DELETED, as actual id list {} does not contain all the expected ids {}", actualIdList, expectedIdList);
            return ResourceStatus.DELETED;
        }
        List<String> actualAttachedIdList = existingDisks.stream().
                filter(Disk::isAttachedToVirtualMachine).map(Disk::id)
                .collect(toList());
        if (actualAttachedIdList.containsAll(expectedIdList)) {
            LOGGER.debug("Resource status is ATTACHED, as attached id list {} contains all the expected ids {}", actualAttachedIdList, expectedIdList);
            return ResourceStatus.ATTACHED;
        }
        LOGGER.debug("Resource status is CREATED, as attached id list {} does not contain all the expected ids {}", actualAttachedIdList, expectedIdList);
        return ResourceStatus.CREATED;
    }

    private AzureClient getAzureClient(AuthenticatedContext auth) {
        return auth.getParameter(AzureClient.class);
    }

    private VolumeSetAttributes getVolumeSetAttributes(CloudResource volumeSet) {
        return volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    @Override
    public int order() {
        return 1;
    }
}
