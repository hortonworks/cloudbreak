package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.GcpRawDiskType;
import com.sequenceiq.cloudbreak.domain.ResourceType;

@Component
public class GcpDiskResourceBuilder extends AbstractGcpComputeBuilder {

    private static final long DEFAULT_ROOT_DISK_SIZE = 50L;

    @Override
    public List<CloudResource> create(GcpContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = getResourceNameService().resourceName(resourceType(), cloudContext.getStackName(), group.getName(), privateId);
        return Arrays.asList(createNamedResource(resourceType(), resourceName));
    }

    @Override
    public List<CloudResource> build(GcpContext context, long privateId, AuthenticatedContext auth, Group group, Image image,
            List<CloudResource> buildableResources) throws Exception {
        String projectId = context.getProjectId();
        CloudRegion region = CloudRegion.valueOf(context.getRegion());

        Disk disk = new Disk();
        disk.setSizeGb(DEFAULT_ROOT_DISK_SIZE);
        disk.setName(buildableResources.get(0).getName());
        disk.setKind(GcpRawDiskType.HDD.getUrl(projectId, region));

        Compute.Disks.Insert insDisk = context.getCompute().disks().insert(projectId, region.value(), disk);
        insDisk.setSourceImage(GcpStackUtil.getAmbariImage(projectId, image.getImageName()));
        try {
            Operation operation = insDisk.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), buildableResources.get(0).getName());
            }
            return Arrays.asList(createOperationAwareCloudResource(buildableResources.get(0), operation));
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResources.get(0).getName());
        }
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String resourceName = resource.getName();
        try {
            Operation operation = context.getCompute().disks()
                    .delete(context.getProjectId(), CloudRegion.valueOf(context.getRegion()).value(), resourceName).execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resourceName, resourceType());
        }
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_DISK;
    }

    @Override
    public int order() {
        return 0;
    }
}
