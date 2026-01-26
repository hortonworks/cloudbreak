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

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.google.api.client.auth.oauth2.TokenResponseException;
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
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.cloudbreak.util.IndexingDeviceNameGenerator;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class GcpAttachedDiskResourceBuilder extends AbstractGcpComputeBuilder {

    private static final String VOLUME_DELETION_FAILED_BASE_MSG = "Volume deletion operation has failed due to:";

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpAttachedDiskResourceBuilder.class);

    private static final String NVME_DEVICE_NAME_TEMPLATE = "/dev/disk/by-id/google-local-nvme-ssd-%d";

    private static final String DEVICE_NAME_PREFIX = "/dev/disk/by-id/google-";

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Override
    public List<CloudResource> create(GcpContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        List<CloudResource> computeResources = context.getComputeResources(privateId);
        Optional<CloudResource> reattachableDiskSet = computeResources.stream()
                .filter(resource -> resourceType().equals(resource.getType()))
                .findFirst();

        return List.of(reattachableDiskSet
                .orElseGet(() -> createAttachedDiskSet(context, privateId, auth, group, instance)));
    }

    private CloudResource createAttachedDiskSet(GcpContext context, long privateId, AuthenticatedContext auth, Group group, CloudInstance instance) {
        InstanceTemplate template = group.getReferenceInstanceTemplate();
        GcpResourceNameService resourceNameService = getResourceNameService();
        String groupName = group.getName();
        CloudContext cloudContext = auth.getCloudContext();
        String stackName = cloudContext.getName();

        List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        IndexingDeviceNameGenerator localSsdDeviceNameGenerator = new IndexingDeviceNameGenerator(NVME_DEVICE_NAME_TEMPLATE, 0);
        for (int i = 0; i < template.getVolumes().size(); i++) {
            String volumeName = resourceNameService.attachedDisk(stackName, groupName, privateId, i);
            Volume volume = template.getVolumes().get(i);

            String diskDeviceName = getDiskDeviceName(volume.getType(), localSsdDeviceNameGenerator, volumeName);

            volumes.add(new VolumeSetAttributes.Volume(volumeName, diskDeviceName,
                    volume.getSize(), volume.getType(), volume.getVolumeUsageType()));
        }
        String resourceName = resourceNameService.attachedDisk(stackName, groupName, privateId, 0);
        Map<String, Object> attributes = new HashMap<>(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                .withAvailabilityZone(instance.getAvailabilityZone())
                .withDeleteOnTermination(Boolean.TRUE)
                .withVolumes(volumes).build()));
        return CloudResource.builder()
                .withType(resourceType())
                .withStatus(CommonStatus.REQUESTED)
                .withName(resourceName)
                .withGroup(groupName)
                .withAvailabilityZone(instance.getAvailabilityZone())
                .withParameters(attributes)
                .build();
    }

    private static String getDiskDeviceName(String volumeType, IndexingDeviceNameGenerator deviceNameGenerator, String volumeName) {
        if (GcpDiskType.LOCAL_SSD.value().equals(volumeType)) {
            return deviceNameGenerator.next();
        } else {
            return DEVICE_NAME_PREFIX + volumeName;
        }
    }

    @Override
    public List<CloudResource> build(GcpContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> resources, CloudStack cloudStack) throws Exception {
        InstanceTemplate template = group.getReferenceInstanceTemplate();

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
                if (GcpDiskType.LOCAL_SSD.value().equals(volume.getType())) {
                    LOGGER.debug("The volume request is Local ssd so skipping it.");
                    continue;
                }
                Map<String, String> labels = gcpLabelUtil.createLabelsFromTags(cloudStack);
                Disk disk = createDisk(projectId, volume, labels, volumeSetAttributes);

                customGcpDiskEncryptionService.addEncryptionKeyToDisk(template, disk);
                Future<Void> submit = intermediateBuilderExecutor.submit(() -> {
                    Insert insDisk = compute.disks().insert(projectId, volumeSetAttributes.getAvailabilityZone(), disk);
                    try {
                        Operation operation = insDisk.execute();
                        syncedOperations.add(operation.getName());
                        if (operation.getHttpErrorStatusCode() != null) {
                            throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), disk.getName());
                        }
                    } catch (TokenResponseException e) {
                        throw gcpStackUtil.getMissingServiceAccountKeyError(e, projectId);
                    } catch (GoogleJsonResponseException e) {
                        throw new GcpResourceException(checkException(e), resourceType(), disk.getName());
                    }
                    return null;
                });
                futures.add(submit);
            }
            volumeSetResource.putParameter(OPERATION_ID, operations);
            result.add(CloudResource.builder()
                    .cloudResource(volumeSetResource)
                    .withStatus(CommonStatus.CREATED)
                    .withParameters(volumeSetResource.getParameters())
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
            resource.setStatus(CommonStatus.DETACHED);
            volumeSetAttributes.setDeleteOnTermination(Boolean.TRUE);
            resource.putParameter(CloudResource.ATTRIBUTES, volumeSetAttributes);
            resourceNotifier.notifyUpdate(resource, auth.getCloudContext());
            throw new PreserveResourceException("Resource will be preserved for later reattachment.");
        }

        List<String> operations = new ArrayList<>();
        List<String> syncedOperations = Collections.synchronizedList(operations);
        Collection<Future<Void>> futures = new ArrayList<>();
        for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
            Future<Void> submit = intermediateBuilderExecutor.submit(() -> {
                try {
                    LOGGER.info("Going to delete volume [id: {}] from project [id: {}] in the following availability zone: {}", volume.getId(),
                            context.getProjectId(), volumeSetAttributes.getAvailabilityZone());
                    Operation operation = context.getCompute().disks()
                            .delete(context.getProjectId(), volumeSetAttributes.getAvailabilityZone(), volume.getId()).execute();
                    syncedOperations.add(operation.getName());
                    if (operation.getHttpErrorStatusCode() != null) {
                        String message = String.format("%s [code: %d, message: %s, error: %s]", VOLUME_DELETION_FAILED_BASE_MSG,
                                operation.getHttpErrorStatusCode(), operation.getHttpErrorMessage(), operation.getError());
                        LOGGER.warn(message);
                        throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), volume.getId());
                    }
                } catch (TokenResponseException e) {
                    logVolumeDeletionProblem(e);
                    gcpStackUtil.getMissingServiceAccountKeyError(e, context.getProjectId());
                } catch (GoogleJsonResponseException e) {
                    logVolumeDeletionProblem(e);
                    exceptionHandler(e, resource.getName(), resourceType());
                } catch (IOException e) {
                    logVolumeDeletionProblem(e);
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
        disk.setDescription(description());
        disk.setSizeGb(Long.valueOf(volume.getSize()));
        disk.setName(volume.getId());
        disk.setType(GcpDiskType.getUrl(projectId, attributes.getAvailabilityZone(), volume.getType()));
        disk.setLabels(tags);
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
                            Operation operation = getResourceChecker().check(context, operationId, resources);
                            return operation == null || gcpStackUtil.isOperationFinished(operation);
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

    private void logVolumeDeletionProblem(Exception e) {
        LOGGER.info(String.format("%s %s", VOLUME_DELETION_FAILED_BASE_MSG, e.getMessage()), e);
    }

}
