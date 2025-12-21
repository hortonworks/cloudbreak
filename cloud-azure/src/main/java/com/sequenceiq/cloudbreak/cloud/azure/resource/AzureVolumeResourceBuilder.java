package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.FQDN;
import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.resources.fluentcore.arm.AvailabilityZoneId;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDisk;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.cloudbreak.constant.AzureConstants;
import com.sequenceiq.cloudbreak.util.IndexingDeviceNameGenerator;
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

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private AzureInstanceFinder azureInstanceFinder;

    @Override
    public List<CloudResource> create(AzureContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.info("Creating volume resources");
        List<CloudResource> computeResources = context.getComputeResources(privateId);
        if (Objects.isNull(computeResources) || computeResources.isEmpty()) {
            return null;
        }
        CloudResource vm = azureInstanceFinder.getInstanceCloudResource(privateId, computeResources);
        Optional<CloudResource> reattachableVolumeSet = getReattachableVolumeSet(computeResources, vm);

        return List.of(reattachableVolumeSet.orElseGet(
                () -> createVolumeSet(privateId, auth, group, vm, context.getStringParameter(PlatformParametersConsts.RESOURCE_CRN_PARAMETER), true,
                        instance.getStringParameter(FQDN))));
    }

    private Optional<CloudResource> getReattachableVolumeSet(List<CloudResource> computeResources, CloudResource vm) {
        LOGGER.debug("Find reattachable volume set for {}", vm);
        Optional<CloudResource> reattachableVolumeSet = computeResources.stream()
                .filter(resource -> ResourceType.AZURE_VOLUMESET.equals(resource.getType()))
                .filter(cloudResource -> CommonStatus.DETACHED.equals(cloudResource.getStatus()) || vm.getInstanceId().equals(cloudResource.getInstanceId()))
                .findFirst();
        reattachableVolumeSet.ifPresent(cloudResource -> cloudResource.setInstanceId(vm.getInstanceId()));
        LOGGER.info("Reattachable volume set {}",
                reattachableVolumeSet.map(cloudResource -> "is present with name:" + cloudResource.getName())
                        .orElse("is not present"));
        return reattachableVolumeSet;
    }

    public CloudResource createVolumeSet(long privateId, AuthenticatedContext auth, Group group, CloudResource vm, String stackCrn,
            boolean withVolumesFromTemplate, String fqdn) {
        String instanceId = vm.getInstanceId();
        Optional<CloudResource> volumeSetForInstanceId = findVolumeSetForInstanceId(instanceId, auth, group);
        if (volumeSetForInstanceId.isPresent()) {
            return volumeSetForInstanceId.get();
        }
        AzureResourceNameService resourceNameService = getResourceNameService();

        InstanceTemplate template = group.getReferenceInstanceTemplate();
        String groupName = group.getName();
        CloudContext cloudContext = auth.getCloudContext();
        String stackName = cloudContext.getName();
        String availabilityZone = getAvailabilityZone(auth, vm);
        String hashableString = stackCrn + System.currentTimeMillis();

        return CloudResource.builder()
                .withInstanceId(instanceId)
                .withPersistent(true)
                .withType(resourceType())
                .withName(resourceNameService.volumeSet(stackName, groupName, privateId, hashableString))
                .withGroup(group.getName())
                .withStatus(CommonStatus.REQUESTED)
                .withAvailabilityZone(availabilityZone)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                                .withAvailabilityZone(availabilityZone)
                                .withDeleteOnTermination(Boolean.TRUE)
                                .withDiscoveryFQDN(fqdn)
                                .withVolumes(
                                        withVolumesFromTemplate ? template.getVolumes().stream()
                                                .map(volume -> new Volume(
                                                        resourceNameService.attachedDisk(stackName, groupName, privateId,
                                                                template.getVolumes().indexOf(volume), hashableString),
                                                        null, volume.getSize(), volume.getType(), volume.getVolumeUsageType()))
                                                .collect(toList()) : new ArrayList<>())
                                .build(),
                        PRIVATE_ID, privateId))
                .build();
    }

    @NotNull
    private Optional<CloudResource> findVolumeSetForInstanceId(String instanceId, AuthenticatedContext auth, Group group) {
        LOGGER.debug("Find volume set for instance id: {}", instanceId);
        List<CloudResource> createdAndRequestedVolumeSets = resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.CREATED,
                ResourceType.AZURE_VOLUMESET, auth.getCloudContext().getId(), group.getName());
        LOGGER.debug("Created volume sets: {}", createdAndRequestedVolumeSets);
        createdAndRequestedVolumeSets.addAll(resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.REQUESTED,
                ResourceType.AZURE_VOLUMESET, auth.getCloudContext().getId(), group.getName()));
        LOGGER.debug("Created and requested volume sets: {}", createdAndRequestedVolumeSets);
        return createdAndRequestedVolumeSets.stream()
                .filter(cr -> Objects.equals(instanceId, cr.getInstanceId()))
                .findFirst();
    }

    private String getAvailabilityZone(AuthenticatedContext auth, CloudResource vm) {
        LOGGER.debug("Fetching availability zone for vm {}", vm.getName());
        AzureClient client = getAzureClient(auth);
        String vmName = vm.getName();
        CloudResource resourceGroup = resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AZURE_RESOURCE_GROUP,
                        auth.getCloudContext().getId()).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find the resource group for the stack"));
        String resourceGroupName = resourceGroup.getName();

        // Azure Java client returns a Set, but VM can only have at most 1 AZ
        return client.getAvailabilityZone(resourceGroupName, vmName).stream()
                .findFirst()
                .map(AvailabilityZoneId::toString)
                .orElse(null);
    }

    @Override
    public List<CloudResource> build(AzureContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        return build(auth, group, buildableResource, cloudStack, null, null);
    }

    public List<CloudResource> build(AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack, Integer offSet, Map<String, String> additionalTags) throws Exception {
        LOGGER.info("Create volumes on provider");

        List<CloudResource> requestedResources = filterRequestedResources(buildableResource);
        Map<String, List<Volume>> volumeSetMap = createVolumesAsync(requestedResources, auth, group, cloudStack, offSet, additionalTags);

        return updateResourcesWithCreatedVolumes(buildableResource, volumeSetMap);
    }

    private List<CloudResource> filterRequestedResources(List<CloudResource> buildableResource) {
        return buildableResource.stream()
                .filter(cloudResource -> CommonStatus.REQUESTED.equals(cloudResource.getStatus()))
                .toList();
    }

    private Map<String, List<Volume>> createVolumesAsync(List<CloudResource> requestedResources, AuthenticatedContext auth,
            Group group, CloudStack cloudStack, Integer offSet, Map<String, String> additionalTags) throws Exception {
        AzureClient client = getAzureClient(auth);
        CloudContext cloudContext = auth.getCloudContext();
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack);
        String region = cloudContext.getLocation().getRegion().getRegionName();
        String diskEncryptionSetId = getDiskEncryptionSetId(group);
        int deviceOffset = offSet != null ? offSet : 0;
        Map<String, String> mergedTags = getTags(cloudStack.getTags(), additionalTags);

        Map<String, List<Volume>> volumeSetMap = Collections.synchronizedMap(new HashMap<>());
        List<Future<?>> futures = new ArrayList<>();

        for (CloudResource resource : requestedResources) {
            VolumeSetAttributes volumeSetAttributes = getVolumeSetAttributes(resource);
            List<Volume> originalVolumes = volumeSetAttributes.getVolumes();

            List<Volume> orderedVolumes = createOrderedVolumeList(originalVolumes.size());
            volumeSetMap.put(resource.getName(), orderedVolumes);

            List<String> deviceNames = generateDeviceNames(originalVolumes, deviceOffset);
            AzureDisk diskTemplate = new AzureDisk(null, 0, null, region, resourceGroupName, mergedTags, diskEncryptionSetId, null);
            submitVolumeCreationTasks(futures, resource, originalVolumes, deviceNames, orderedVolumes, diskTemplate, client);
        }

        awaitVolumeCreation(futures);
        return volumeSetMap;
    }

    private List<Volume> createOrderedVolumeList(int size) {
        return Collections.synchronizedList(new ArrayList<>(Collections.nCopies(size, null)));
    }

    private List<String> generateDeviceNames(List<Volume> volumes, int deviceOffset) {
        IndexingDeviceNameGenerator generator = new IndexingDeviceNameGenerator(AzureConstants.LUN_DEVICE_NAME_TEMPLATE, deviceOffset);
        List<String> deviceNames = new ArrayList<>(volumes.size());
        for (Volume volume : volumes) {
            deviceNames.add(volume.getDevice() != null ? volume.getDevice() : generator.next());
        }
        return deviceNames;
    }

    private void submitVolumeCreationTasks(List<Future<?>> futures, CloudResource resource, List<Volume> originalVolumes,
            List<String> deviceNames, List<Volume> orderedVolumes, AzureDisk diskTemplate, AzureClient client) {
        for (int index = 0; index < originalVolumes.size(); index++) {
            final int volumeIndex = index;
            Volume originalVolume = originalVolumes.get(volumeIndex);

            Future<?> future = intermediateBuilderExecutor.submit(() ->
                createVolumeAndPlaceAtIndex(originalVolume, volumeIndex, deviceNames, orderedVolumes, resource, diskTemplate, client));
            futures.add(future);
        }
    }

    private void createVolumeAndPlaceAtIndex(Volume originalVolume, int volumeIndex, List<String> deviceNames,
            List<Volume> orderedVolumes, CloudResource resource, AzureDisk diskTemplate, AzureClient client) {
        Disk disk = getOrCreateDisk(originalVolume, resource, diskTemplate, client);
        Volume createdVolume = new Volume(
                disk.id(), deviceNames.get(volumeIndex), originalVolume.getSize(),
                originalVolume.getType(), originalVolume.getCloudVolumeUsageType());
        orderedVolumes.set(volumeIndex, createdVolume);
    }

    private Disk getOrCreateDisk(Volume originalVolume, CloudResource resource, AzureDisk diskTemplate, AzureClient client) {
        String diskName = getDiskName(originalVolume.getId());
        Disk existingDisk = client.getDiskByName(diskTemplate.getResourceGroupName(), diskName);
        if (existingDisk != null) {
            LOGGER.info("Managed disk for resource group: {}, name: {} already exists: {}",
                    diskTemplate.getResourceGroupName(), originalVolume.getId(), existingDisk);
            return existingDisk;
        } else {
            AzureDisk azureDisk = new AzureDisk(originalVolume.getId(), originalVolume.getSize(),
                    AzureDiskType.getByValue(originalVolume.getType()), diskTemplate.getRegion(),
                    diskTemplate.getResourceGroupName(), diskTemplate.getTags(),
                    diskTemplate.getDiskEncryptionSetId(), resource.getAvailabilityZone());
            return client.createManagedDisk(azureDisk);
        }
    }

    private void awaitVolumeCreation(List<Future<?>> futures) throws Exception {
        LOGGER.debug("Waiting for volumes creation requests ({} futures)", futures.size());
        for (Future<?> future : futures) {
            future.get();
        }
    }

    private List<CloudResource> updateResourcesWithCreatedVolumes(List<CloudResource> buildableResource,
            Map<String, List<Volume>> volumeSetMap) {
        return buildableResource.stream()
                .map(resource -> updateResourceWithVolumes(resource, volumeSetMap.get(resource.getName())))
                .collect(toList());
    }

    private CloudResource updateResourceWithVolumes(CloudResource resource, List<Volume> volumes) {
        if (!CollectionUtils.isEmpty(volumes)) {
            getVolumeSetAttributes(resource).setVolumes(volumes);
        }
        resource.setStatus(CommonStatus.CREATED);
        return resource;
    }

    private String getDiskEncryptionSetId(Group group) {
        CloudInstance cloudInstance = group.getReferenceInstanceConfiguration();
        AzureInstanceView azureInstanceView = AzureInstanceView.builder(cloudInstance).build();
        return azureInstanceView.isManagedDiskEncryptionWithCustomKeyEnabled() ? azureInstanceView.getDiskEncryptionSetId() : null;
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
                        ResourceStatus.ATTACHED.equals(cloudResourceStatus.getStatus()) && volumeSetAttributes.getDeleteOnTermination())
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
            detachVolumes(client, cloudResource, volumeIds);
            LOGGER.info("Going to attempt to delete the following managed disks (based on the following IDs) on Azure: {}",
                    String.join(",", volumeIds));
            azureUtils.deleteManagedDisks(client, volumeIds);
        }
    }

    private List<String> getVolumeIds(CloudResource cloudResource) {
        return getVolumeSetAttributes(cloudResource).getVolumes().stream()
                .map(Volume::getId)
                .collect(toList());
    }

    private void detachVolumes(AzureClient client, CloudResource cloudResource, List<String> volumeIds) {
        String instanceName = cloudResource.getInstanceId();
        Multimap<String, String> vmDiskMap = ArrayListMultimap.create();
        for (String volumeId : volumeIds) {
            Disk diskById = client.getDiskById(volumeId);
            if (diskById.isAttachedToVirtualMachine()) {
                String vmId = diskById.virtualMachineId();
                LOGGER.info("Volume {} is attached to {}", volumeId, vmId);
                vmDiskMap.put(vmId, volumeId);
            } else {
                LOGGER.info("No need to detach disk(VolumeId:'{}') as it is not attached to any VM", volumeId);
            }
        }
        LOGGER.info("VM disk map: {}", vmDiskMap);
        for (String vm : vmDiskMap.keySet()) {
            VirtualMachine virtualMachine = client.getVirtualMachine(vm);
            if (Objects.equals(virtualMachine.name(), instanceName)) {
                try {
                    client.detachDisksFromVm(vmDiskMap.get(vm), virtualMachine);
                } catch (RuntimeException e) {
                    LOGGER.warn("Can not detach " + volumeIds + " from " + instanceName, e);
                }
            } else {
                LOGGER.warn("{} disks are not attached to the correct VM: {}", vmDiskMap.get(vm), instanceName);
            }
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

        List<Disk> existingDisks = client.listDisksByResourceGroup(resourceGroupName).getAll();
        ResourceStatus volumeSetStatus = getResourceStatus(existingDisks, volumeResources);
        return volumeResources.stream()
                .map(resource -> new CloudResourceStatus(resource, volumeSetStatus))
                .collect(toList());
    }

    protected void modifyVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) {
        AzureClient client = getAzureClient(authenticatedContext);
        for (String volumeId : volumeIds) {
            // AZURE VOL IDs are always a combination of subscription, resource group and disk name in that order
            String resourceGroupName = StringUtils.substringBetween(volumeId, "resourceGroups/", "/providers");
            String diskName = volumeId.substring(volumeId.lastIndexOf("/") + 1);
            client.modifyDisk(diskName, resourceGroupName, size, diskType);
        }
    }

    protected void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) {
        AzureClient client = getAzureClient(authenticatedContext);
        for (CloudResource cloudResource : cloudResources) {
            List<String> volumeIds = getVolumeIds(cloudResource);
            LOGGER.info("VolumeIds to detach: {}", String.join(", ", volumeIds));
            detachVolumes(client, cloudResource, volumeIds);
        }
    }

    protected void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) {
        AzureClient client = getAzureClient(authenticatedContext);
        for (CloudResource cloudResource : cloudResources) {
            List<String> volumeIds = getVolumeIds(cloudResource);
            LOGGER.info("Going to attempt to delete the following managed disks (based on the following IDs) on Azure: {}",
                    String.join(",", volumeIds));
            azureUtils.deleteManagedDisks(client, volumeIds);
        }
    }

    private ResourceStatus getResourceStatus(List<Disk> existingDisks, List<CloudResource> volumeResources) {
        List<String> expectedIdList = volumeResources.stream()
                .map(this::getVolumeSetAttributes)
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .map(Volume::getId)
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

    private Map<String, String> getTags(Map<String, String> stackTags, Map<String, String> additionalTags) {
        if (stackTags != null && additionalTags != null) {
            Map<String, String> mergedTags = new HashMap<>(stackTags);
            mergedTags.putAll(additionalTags);
            return mergedTags;
        }
        return stackTags != null ? stackTags : additionalTags;
    }

    private String getDiskName(String volumeId) {
        return volumeId.lastIndexOf('/') != -1 ? volumeId.substring(volumeId.lastIndexOf('/') + 1) : volumeId;
    }

    @Override
    public int order() {
        return 1;
    }
}
