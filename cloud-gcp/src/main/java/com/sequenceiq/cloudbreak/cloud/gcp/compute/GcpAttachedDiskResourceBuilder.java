package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters.GcpDiskType;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class GcpAttachedDiskResourceBuilder extends AbstractGcpComputeBuilder {

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Override
    public List<CloudResource> create(GcpContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        List<CloudResource> cloudResources = new ArrayList<>();
        CloudInstance instance = group.getReferenceInstanceConfiguration();
        InstanceTemplate template = instance.getTemplate();
        GcpResourceNameService resourceNameService = getResourceNameService();
        String groupName = group.getName();
        final CloudContext cloudContext = auth.getCloudContext();
        final String stackName = cloudContext.getName();
        for (int i = 0; i < template.getVolumes().size(); i++) {
            final String resourceName = resourceNameService.resourceName(resourceType(), stackName, groupName, privateId, i);
            cloudResources.add(createNamedResource(resourceType(), resourceName));
        }
        return cloudResources;
    }

    @Override
    public List<CloudResource> build(GcpContext context, long privateId, final AuthenticatedContext auth, Group group, Image image,
            List<CloudResource> buildableResource) throws Exception {
        CloudInstance instance = group.getReferenceInstanceConfiguration();
        InstanceTemplate template = instance.getTemplate();
        Volume volume = template.getVolumes().get(0);

        List<CloudResource> resources = new ArrayList<>();
        final List<CloudResource> syncedResources = Collections.synchronizedList(resources);
        final String projectId = context.getProjectId();
        final Location location = context.getLocation();
        final Compute compute = context.getCompute();
        List<Future<Void>> futures = new ArrayList<>();
        for (final CloudResource cloudResource : buildableResource) {
            final Disk disk = createDisk(volume, projectId, location.getAvailabilityZone(), cloudResource.getName());
            Future<Void> submit = intermediateBuilderExecutor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    Compute.Disks.Insert insDisk = compute.disks().insert(projectId, location.getAvailabilityZone().value(), disk);
                    try {
                        Operation operation = insDisk.execute();
                        syncedResources.add(createOperationAwareCloudResource(cloudResource, operation));
                        if (operation.getHttpErrorStatusCode() != null) {
                            throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), cloudResource.getName());
                        }
                    } catch (GoogleJsonResponseException e) {
                        throw new GcpResourceException(checkException(e), resourceType(), cloudResource.getName());
                    }
                    return null;
                }
            });
            futures.add(submit);
        }
        for (Future<Void> future : futures) {
            future.get();
        }
        return resources;
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String resourceName = resource.getName();
        try {
            Operation operation = context.getCompute().disks()
                    .delete(context.getProjectId(), context.getLocation().getAvailabilityZone().value(), resourceName).execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resourceName, resourceType());
        }
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_ATTACHED_DISK;
    }

    @Override
    public int order() {
        return 1;
    }

    private Disk createDisk(Volume volume, String projectId, AvailabilityZone availabilityZone, String resourceName) {
        Disk disk = new Disk();
        disk.setSizeGb((long) volume.getSize());
        disk.setName(resourceName);
        disk.setType(GcpDiskType.getUrl(projectId, availabilityZone, volume.getType()));
        return disk;
    }
}
