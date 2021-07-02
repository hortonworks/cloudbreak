package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute.Disks.Insert;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters.GcpDiskType;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class GcpDiskResourceBuilder extends AbstractGcpComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDiskResourceBuilder.class);

    @Inject
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public List<CloudResource> create(GcpContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = getResourceNameService().resourceName(resourceType(), cloudContext.getName(), group.getName(), privateId);
        return Collections.singletonList(createNamedResource(resourceType(), resourceName));
    }

    @Override
    public List<CloudResource> build(GcpContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResources, CloudStack cloudStack) throws Exception {
        String projectId = context.getProjectId();
        Location location = context.getLocation();

        Disk disk = new Disk();
        disk.setDescription(description());
        disk.setSizeGb((long) group.getRootVolumeSize());
        disk.setName(buildableResources.get(0).getName());
        disk.setKind(GcpDiskType.SSD.getUrl(projectId, location.getAvailabilityZone()));

        InstanceTemplate template = group.getReferenceInstanceTemplate();
        customGcpDiskEncryptionService.addEncryptionKeyToDisk(template, disk);

        Map<String, String> labels = gcpLabelUtil.createLabelsFromTags(cloudStack);
        disk.setLabels(labels);

        Insert insDisk = context.getCompute().disks().insert(projectId, location.getAvailabilityZone().value(), disk);
        insDisk.setSourceImage(gcpStackUtil.getAmbariImage(projectId, cloudStack.getImage().getImageName()));
        try {
            Operation operation = insDisk.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), buildableResources.get(0).getName());
            }
            return Collections.singletonList(createOperationAwareCloudResource(buildableResources.get(0), operation));
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResources.get(0).getName());
        }
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String resourceName = resource.getName();
        try {
            LOGGER.info("Creating operation to delete disk [name: {}] in project [id: {}] in the following availability zone: {}", resourceName,
                    context.getProjectId(), context.getLocation().getAvailabilityZone().value());
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
        return ResourceType.GCP_DISK;
    }

    @Override
    public int order() {
        return 0;
    }
}
