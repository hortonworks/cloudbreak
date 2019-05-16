package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Disks.Insert;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters.GcpDiskType;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.util.DeviceNameGenerator;

@Component
public class GcpAttachedDiskResourceBuilder extends AbstractGcpComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpAttachedDiskResourceBuilder.class);

    private static final String DEVICE_NAME_TEMPLATE = "/dev/sd%s";

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    @Inject
    private GcpDiskEncryptionService gcpDiskEncryptionService;

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Override
    public List<CloudResource> create(GcpContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        List<CloudResource> computeResources = context.getComputeResources(privateId);
        Optional<CloudResource> reattachableDiskSet = computeResources.stream()
                .filter(resource -> resourceType().equals(resource.getType()))
                .findFirst();

        return List.of(reattachableDiskSet
                .orElseGet(() -> createAttachedDiskSet(context, privateId, auth, group)));
    }

    private CloudResource createAttachedDiskSet(GcpContext context, long privateId, AuthenticatedContext auth, Group group) {
        CloudInstance instance = group.getReferenceInstanceConfiguration();
        InstanceTemplate template = instance.getTemplate();
        GcpResourceNameService resourceNameService = getResourceNameService();
        String groupName = group.getName();
        CloudContext cloudContext = auth.getCloudContext();
        String stackName = cloudContext.getName();
        Location location = context.getLocation();

        List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        DeviceNameGenerator generator = new DeviceNameGenerator(DEVICE_NAME_TEMPLATE);
        for (int i = 0; i < template.getVolumes().size(); i++) {
            String volumeName = resourceNameService.resourceName(resourceType(), stackName, groupName, privateId, i);
            Volume volume = template.getVolumes().get(i);
            volumes.add(new VolumeSetAttributes.Volume(volumeName, generator.next(), volume.getSize(), volume.getType()));
        }
        String resourceName = resourceNameService.resourceName(resourceType(), stackName, groupName, privateId, 0);
        Map<String, Object> attributes = new HashMap<>(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                .withAvailabilityZone(location.getAvailabilityZone().value())
                .withDeleteOnTermination(Boolean.TRUE)
                .withVolumes(volumes).build()));
        return new Builder()
                .type(resourceType())
                .status(CommonStatus.REQUESTED)
                .name(resourceName)
                .group(groupName)
                .params(attributes)
                .build();
    }

    @Override
    public List<CloudResource> build(GcpContext context, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> resources, CloudStack cloudStack) throws Exception {
        CloudInstance instance = group.getReferenceInstanceConfiguration();
        InstanceTemplate template = instance.getTemplate();

        List<String> operations = new ArrayList<>();
        List<String> syncedOperations = Collections.synchronizedList(operations);
        String projectId = context.getProjectId();
        Compute compute = context.getCompute();
        Collection<Future<Void>> futures = new ArrayList<>();

        List<CloudResource> buildableResource = resources.stream()
                .filter(cloudResource -> CommonStatus.REQUESTED.equals(cloudResource.getStatus()))
                .collect(Collectors.toList());

        List<CloudResource> result = new ArrayList<>();
        for (CloudResource volumeSetResource : buildableResource) {
            VolumeSetAttributes volumeSetAttributes = volumeSetResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);

            for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
                Disk disk = createDisk(projectId, volume, cloudStack.getTags(), volumeSetAttributes);

                gcpDiskEncryptionService.addEncryptionKeyToDisk(template, disk);
                Future<Void> submit = intermediateBuilderExecutor.submit(() -> {
                    Insert insDisk = compute.disks().insert(projectId, volumeSetAttributes.getAvailabilityZone(), disk);
                    try {
                        Operation operation = insDisk.execute();
                        syncedOperations.add(operation.getName());
                        if (operation.getHttpErrorStatusCode() != null) {
                            throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), disk.getName());
                        }
                    } catch (GoogleJsonResponseException e) {
                        throw new GcpResourceException(checkException(e), resourceType(), disk.getName());
                    }
                    return null;
                });
                futures.add(submit);
            }
            volumeSetResource.putParameter(OPERATION_ID, operations);
            result.add(new Builder().cloudResource(volumeSetResource)
                    .status(CommonStatus.CREATED)
                    .params(volumeSetResource.getParameters())
                    .build());
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        result.addAll(resources.stream().filter(cloudResource -> CommonStatus.CREATED.equals(cloudResource.getStatus())).collect(Collectors.toList()));
        return result;
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);

        if (!volumeSetAttributes.getDeleteOnTermination()) {
            resource.setInstanceId(null);
            volumeSetAttributes.setDeleteOnTermination(Boolean.TRUE);
            resource.putParameter(CloudResource.ATTRIBUTES, volumeSetAttributes);
            resourceNotifier.notifyUpdate(resource, auth.getCloudContext());
            throw new InterruptedException("Resource will be preserved for later reattachment.");
        }

        List<String> operations = new ArrayList<>();
        List<String> syncedOperations = Collections.synchronizedList(operations);
        Collection<Future<Void>> futures = new ArrayList<>();
        for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
            Future<Void> submit = intermediateBuilderExecutor.submit(() -> {
                try {
                    Operation operation = context.getCompute().disks()
                            .delete(context.getProjectId(), volumeSetAttributes.getAvailabilityZone(), volume.getId()).execute();
                    syncedOperations.add(operation.getName());
                    if (operation.getHttpErrorStatusCode() != null) {
                        throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), volume.getId());
                    }
                } catch (GoogleJsonResponseException e) {
                    exceptionHandler(e, resource.getName(), resourceType());
                } catch (IOException e) {
                    throw new GcpResourceException(e.getMessage(), e);
                }
                return null;
            });
            futures.add(submit);
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        resource.putParameter(OPERATION_ID, operations);
        return resource;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_ATTACHED_DISKSET;
    }

    @Override
    public int order() {
        return 1;
    }

    private Disk createDisk(String projectId, VolumeSetAttributes.Volume volume, Map<String, String> tags, VolumeSetAttributes attributes) {
        Disk disk = new Disk();
        disk.setSizeGb(Long.valueOf(volume.getSize()));
        disk.setName(volume.getId());
        disk.setType(GcpDiskType.getUrl(projectId, attributes.getAvailabilityZone(), volume.getType()));

        Map<String, String> customTags = new HashMap<>();
        customTags.putAll(tags);
        customTags.putAll(defaultCostTaggingService.prepareDiskTagging());
        disk.setLabels(customTags);

        return disk;
    }

    @Override
    public List<CloudResourceStatus> checkResources(GcpContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            LOGGER.debug("Check {} resource: {}", resourceType(), resource);
            List<String> operationIds = Optional.ofNullable(resource.getParameter(OPERATION_ID, List.class)).orElse(List.of());

            boolean finished = operationIds.isEmpty() || operationIds.stream()
                    .allMatch(operationId -> {
                        try {
                            Operation operation = check(context, operationId);
                            return operation == null || GcpStackUtil.isOperationFinished(operation);
                        } catch (Exception e) {
                            CloudContext cloudContext = auth.getCloudContext();
                            throw new GcpResourceException("Error during status check", resourceType(),
                                    cloudContext.getName(), cloudContext.getId(), resource.getName(), e);
                        }
                    });
            ResourceStatus successStatus = context.isBuild() ? ResourceStatus.CREATED : ResourceStatus.DELETED;
            result.add(new CloudResourceStatus(resource, finished ? successStatus : ResourceStatus.IN_PROGRESS));
            if (finished) {
                if (successStatus == ResourceStatus.CREATED) {
                    LOGGER.debug("Creation of {} was successful", resource);
                } else {
                    LOGGER.debug("Deletion of {} was successful", resource);
                }
            }
        }
        return result;
    }
}
