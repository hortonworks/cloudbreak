package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.AbstractOpenStackResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.service.OpenStackResourceNameService;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;

public abstract class AbstractOpenStackComputeResourceBuilder extends AbstractOpenStackResourceBuilder implements ComputeResourceBuilder<OpenStackContext> {

    private static final Map<Class<? extends AbstractOpenStackComputeResourceBuilder>, Integer> ORDER;

    static {
        Map<Class<? extends AbstractOpenStackComputeResourceBuilder>, Integer> map = Maps.newHashMap();
        map.put(OpenStackPortBuilder.class, 0);
        map.put(OpenStackAttachedDiskResourceBuilder.class, 0);
        map.put(OpenStackInstanceBuilder.class, 1);
        map.put(OpenStackFloatingIPBuilder.class, 2);
        ORDER = Collections.unmodifiableMap(map);
    }

    @Inject
    private OpenStackResourceNameService resourceNameService;

    @Override
    public List<CloudResource> create(OpenStackContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = resourceNameService.resourceName(resourceType(), cloudContext.getName(), group.getName(), privateId);
        return Arrays.asList(createNamedResource(resourceType(), resourceName));
    }

    @Override
    public List<CloudVmInstanceStatus> checkInstances(OpenStackContext context, AuthenticatedContext auth, List<CloudInstance> instances) {
        return null;
    }

    @Override
    public CloudVmInstanceStatus start(OpenStackContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public CloudVmInstanceStatus stop(OpenStackContext context, AuthenticatedContext auth, CloudInstance instance) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> checkResources(OpenStackContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    @Override
    public int order() {
        Integer order = ORDER.get(getClass());
        if (order == null) {
            throw new OpenStackResourceException(String.format("No resource order found for class: %s", getClass()));
        }
        return order;
    }

    protected InstanceTemplate getInstanceTemplate(Group group, long privateId) {
        for (CloudInstance instance : group.getInstances()) {
            InstanceTemplate template = instance.getTemplate();
            if (template.getPrivateId().longValue() == privateId) {
                return template;
            }
        }
        return null;
    }
}
