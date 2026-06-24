package com.sequenceiq.cloudbreak.cloud.openstack.compute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import jakarta.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.Volume.Status;
import org.openstack4j.model.storage.block.VolumeAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackResourceNameService;
import com.sequenceiq.cloudbreak.cloud.openstack.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.view.CinderVolumeView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class OpenStackAttachedDiskResourceBuilder extends AbstractOpenStackComputeResourceBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackAttachedDiskResourceBuilder.class);

    @Inject
    private OpenStackResourceNameService resourceNameService;

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Override
    public List<CloudResource> create(OpenStackContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        InstanceTemplate template = getInstanceTemplate(group, privateId);
        NovaInstanceView instanceView = new NovaInstanceView(context.getName(), template, group.getType(), group.getLoginUserName());
        String groupName = group.getName();
        String stackName = getUtils().getStackName(auth);

        List<CloudResource> computeResources = context.getComputeResources(privateId);
        if (computeResources != null) {
            for (CloudResource computeResource : computeResources) {
                if (computeResource.getType() == ResourceType.OPENSTACK_ATTACHED_DISK) {
                    LOGGER.info("Volume {} already exists", computeResource.getName());
                    return Collections.singletonList(computeResource);
                }
            }
        }

        if (!instanceView.getVolumes().isEmpty()) {
            LOGGER.info("Creating attached disks for instance: [{}]", instanceView.getName());
            String resourceName = resourceNameService.resourceName(resourceType(), stackName, groupName, privateId);
            CloudResource resource = createNamedResource(resourceType(), groupName, resourceName);
            List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
            for (CinderVolumeView volumeView : instanceView.getVolumes()) {
                volumes.add(new VolumeSetAttributes.Volume(null, volumeView.getDevice(),
                        volumeView.getSize(), volumeView.getType(), volumeView.getVolumeUsageType()));
            }
            resource.putParameter(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                    .withAvailabilityZone(instance.getAvailabilityZone())
                    .withDeleteOnTermination(Boolean.TRUE)
                    .withVolumes(volumes)
                    .build());
            return Collections.singletonList(resource);
        }
        return Collections.emptyList();
    }

    @Override
    public List<CloudResource> build(OpenStackContext c, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        List<CloudResource> resources = new ArrayList<>();
        Collection<Future<Void>> futures = new ArrayList<>();
        for (CloudResource cloudResource : buildableResource) {
            if (cloudResource.hasParameter(CloudResource.ATTRIBUTES) && cloudResource.getType() == ResourceType.OPENSTACK_ATTACHED_DISK) {
                VolumeSetAttributes volumeSetAttributes = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
                if (volumeSetAttributes != null) {
                    for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
                            Future<Void> submit = intermediateBuilderExecutor.submit(() -> {
                                try {
                                    createOrReuseVolume(auth, cloudResource, volume);
                                } catch (OS4JException ex) {
                                    throw new OpenStackResourceException("Volume creation failed", resourceType(), cloudResource.getName(), ex);
                                }
                                return null;
                            });
                            futures.add(submit);
                    }
                } else {
                    LOGGER.info("Volume attributes are not set for resource: [{}]", cloudResource.getName());
                }
                cloudResource.putParameter(CloudResource.ATTRIBUTES, volumeSetAttributes);
                cloudResource.setStatus(CommonStatus.CREATED);
                resources.add(cloudResource);
            }
        }
        for (Future<Void> future : futures) {
            future.get();
        }
        return resources;
    }

    private void createOrReuseVolume(AuthenticatedContext auth, CloudResource cloudResource, VolumeSetAttributes.Volume volume) {
        OSClient<?> osClient = createOSClient(auth);
        if (volume.getId() == null) {
            String volumeName = String.format("%s-%s", cloudResource.getName(), volume.getDevice().replaceAll("[^a-zA-Z0-9_-]", ""));
            osClient.blockStorage().volumes().list(Map.of("name", volumeName)).stream().findFirst().ifPresentOrElse(cinderVolume -> {
                LOGGER.info("Volume with name: [{}] already exists", volumeName);
                volume.setId(cinderVolume.getId());
                detachIfAttached(cloudResource, volume, osClient);
            }, () -> {
                Volume osVolume = Builders.volume().name(volumeName).size(volume.getSize()).build();
                osVolume = osClient.blockStorage().volumes().create(osVolume);
                volume.setId(osVolume.getId());
            });
        } else {
            detachIfAttached(cloudResource, volume, osClient);
        }
    }

    private void detachIfAttached(CloudResource cloudResource, VolumeSetAttributes.Volume volume, OSClient<?> osClient) {
        Volume cinderVolume = osClient.blockStorage().volumes().get(volume.getId());
        if (cinderVolume != null) {
        List<? extends VolumeAttachment> attachments = cinderVolume.getAttachments();
        if (cloudResource.getInstanceId() != null) {
            osClient.compute().servers().detachVolume(cloudResource.getInstanceId(), volume.getId());
        } else {
            for (VolumeAttachment attachment : attachments) {
                osClient.blockStorage().volumes().detach(attachment.getVolumeId(), attachment.getAttachmentId());
            }
        }
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        try {
            OSClient<?> osClient = createOSClient(auth);
            LOGGER.info("About to delete volume: [{}]", resource);
            VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            if (volumeSetAttributes != null) {
                if (Boolean.TRUE.equals(volumeSetAttributes.getDeleteOnTermination())) {
                    deleteVolume(auth, resource, volumeSetAttributes, osClient);
                } else {
                    preserveVolume(auth, resource, volumeSetAttributes, osClient);
                }
            }
            return resource;
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Volume deletion failed", resourceType(), resource.getName(), ex);
        }
    }

    private void deleteVolume(AuthenticatedContext auth, CloudResource resource, VolumeSetAttributes volumeSetAttributes, OSClient<?> osClient) {
        for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
            if (volume.getId() != null) {
                ActionResponse response = osClient.blockStorage().volumes().delete(volume.getId());
                checkDeleteResponse(response, resourceType(), auth, resource, "Volume deletion failed");
            }
        }
    }

    private void preserveVolume(AuthenticatedContext auth, CloudResource resource, VolumeSetAttributes volumeSetAttributes, OSClient<?> osClient)
            throws PreserveResourceException {
        for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
            Volume openstackVolume = osClient.blockStorage().volumes().get(volume.getId());
            if (openstackVolume != null) {
                for (VolumeAttachment attachment : openstackVolume.getAttachments()) {
                    osClient.compute().servers().detachVolume(attachment.getServerId(), attachment.getAttachmentId());
                    osClient.blockStorage().volumes().detach(attachment.getVolumeId(), attachment.getAttachmentId());
                }
            }
        }
        resource.setStatus(CommonStatus.DETACHED);
        volumeSetAttributes.setDeleteOnTermination(Boolean.TRUE);
        resource.putParameter(CloudResource.ATTRIBUTES, volumeSetAttributes);
        resourceNotifier.notifyUpdate(resource, auth.getCloudContext());
        throw new PreserveResourceException("Resource will be preserved for later reattachment.");
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_ATTACHED_DISK;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        OSClient<?> osClient = createOSClient(auth);
        VolumeSetAttributes volumeSetAttributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        List<Status> foundVolumeStatuses = new ArrayList<>();
        List<VolumeSetAttributes.Volume> notFoundVolumes = new ArrayList<>();
        List<VolumeSetAttributes.Volume> volumes = volumeSetAttributes.getVolumes();
        for (VolumeSetAttributes.Volume volume : volumes) {
            Volume osVolume = osClient.blockStorage().volumes().get(volume.getId());
            if (osVolume != null) {
                Status volumeStatus = osVolume.getStatus();
                if (Status.UNRECOGNIZED.equals(volumeStatus)) {
                    throw new OpenStackResourceException("Volume status is unrecognized");
                }
                foundVolumeStatuses.add(volumeStatus);
            } else {
                LOGGER.info("Volume with id: [{}] not found", volume.getId());
                notFoundVolumes.add(volume);
            }
        }
        if (context.isBuild()) {
            return foundVolumeStatuses.size() == volumes.size() && foundVolumeStatuses.stream()
                    .allMatch(status -> status == Status.AVAILABLE || status == Status.IN_USE);
        } else {
            return notFoundVolumes.size() == volumes.size();
        }
    }
}
