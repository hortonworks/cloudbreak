package com.sequenceiq.cloudbreak.cloud.gcp.group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute.InstanceGroups.Delete;
import com.google.api.services.compute.Compute.InstanceGroups.Insert;
import com.google.api.services.compute.model.InstanceGroup;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * The Group Resource Builder that is responsible for the GCP API calls to manage an instance group
 * This currently only defines unmanaged instance groups
 * In GCP an instance group is a collection of Compute Instances in the same Zone and subnet
 * An unmanaged instace group is only used to be applied as a target of a load balancer backend
 * An instance shoup be added to at most 1 load balanced instance group
 * This creates a Instance Group for every group defined in a stack.
 */

@Service
public class GcpInstanceGroupResourceBuilder extends AbstractGcpGroupBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceGroupResourceBuilder.class);

    private static final int ORDER = 1;

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Group group, Network network, String availabilityZone) {
        String resourceName = getResourceNameService().group(context.getName(), group.getName(), auth.getCloudContext().getId(), availabilityZone);
        return createNamedResource(resourceType(), resourceName, availabilityZone, group.getName());
    }

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Group group, Network network) {
        return create(context, auth, group, network, null);
    }

    @Override
    public CloudResource build(GcpContext context,
            AuthenticatedContext auth, Group group,
            Network network, Security security, CloudResource resource) throws Exception {
        LOGGER.info("Building GCP instancegroup {} for project {}", group.getName(), context.getProjectId());

        Insert insert = context.getCompute().instanceGroups().insert(context.getProjectId(),
                resource.getAvailabilityZone(), new InstanceGroup().setName(resource.getName()));

        return executeOperationalRequest(resource, insert);
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        LOGGER.info("Deleting GCP instancegroup {} for project {}", resource.getName(), context.getProjectId());

        Delete delete = context.getCompute().instanceGroups().delete(context.getProjectId(),
                resource.getAvailabilityZone(), resource.getName());
        try {
            Operation operation = delete.execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            LOGGER.error("unable to delete instance group {}, error: {}", resource.getName(), e.getMessage());
            exceptionHandler(e, resource.getName(), resourceType());
            return null;
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_INSTANCE_GROUP;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public boolean isZonalResource() {
        return true;
    }
}
