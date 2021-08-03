package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Addresses.Insert;
import com.google.api.services.compute.model.Address;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpReservedIpResourceBuilder extends AbstractGcpComputeBuilder {

    public static final String INTERNAL = "INTERNAL";

    public static final String EXTERNAL = "EXTERNAL";

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpReservedIpResourceBuilder.class);

    @Override
    public List<CloudResource> create(GcpContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        if (group.getType() == InstanceGroupType.GATEWAY && !context.getNoPublicIp()) {
            String resourceName = getResourceNameService().resourceName(resourceType(), auth.getCloudContext().getName(), group.getName(), privateId);
            return Collections.singletonList(createNamedResource(resourceType(), resourceName, instance.getAvailabilityZone()));
        } else {
            LOGGER.debug("Public IPs for instances in {} group won't be created, because it is not a gateway instancegroup or no public ip is needed.",
                    group.getName());
            return Collections.emptyList();
        }
    }

    @Override
    public List<CloudResource> build(GcpContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        return buildReservedIp(context, buildableResource, cloudStack, EXTERNAL);
    }

    public List<CloudResource> buildReservedIp(GcpContext context, List<CloudResource> buildableResource,
            CloudStack cloudStack, String type) throws Exception {
        List<CloudResource> result = new ArrayList<>();
        for (CloudResource resource : buildableResource) {
            String projectId = context.getProjectId();
            String region = context.getLocation().getRegion().value();

            Address address = new Address();
            address.setName(resource.getName());
            address.setDescription(description());
            address.setAddressType(type);

            Map<String, Object> customTags = new HashMap<>();
            customTags.putAll(cloudStack.getTags());
            address.setUnknownKeys(customTags);
            Insert networkInsert = context.getCompute().addresses().insert(projectId, region, address);
            try {
                Operation operation = networkInsert.execute();
                if (operation.getHttpErrorStatusCode() != null) {
                    throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), resource.getName());
                }
                result.add(createOperationAwareCloudResource(resource, operation));
            } catch (GoogleJsonResponseException e) {
                throw new GcpResourceException(checkException(e), resourceType(), resource.getName());
            }
        }
        return result;

    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        return deleteReservedIP(context, resource);
    }

    public CloudResource deleteReservedIP(GcpContext context, CloudResource resource) throws Exception {
        Compute compute = context.getCompute();
        String projectId = context.getProjectId();
        String region = context.getLocation().getRegion().value();
        try {
            LOGGER.info("About to delete GCP reserved ip address in: [projectID: {}, region: {}, resource: {}]", projectId, region, resource.getName());
            Operation operation = compute.addresses().delete(projectId, region, resource.getName()).execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resource.getName(), resourceType());
            return null;
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_RESERVED_IP;
    }

    @Override
    public int order() {
        return 2;
    }
}
