package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.Instance;

@Component
public class AwsRootVolumeResourceBuilder extends AbstractAwsComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRootVolumeResourceBuilder.class);

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    private VolumeBuilderUtil volumeBuilderUtil;

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        String resourceName = getResourceNameService().rootDisk(context.getName(), auth.getCloudContext().getId(), group.getName(),
                privateId);
        LOGGER.info("Creating AWS_ROOT_DISK resource with resource name " + resourceName);
        return List.of(volumeBuilderUtil.createRootVolumeResource(resourceName, group.getName(), ResourceType.AWS_ROOT_DISK,
                instance.getAvailabilityZone()));
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        String instanceId = context.getComputeResources(privateId).getFirst().getInstanceId();
        LOGGER.info("Updating AWS_ROOT_DISK resource with resource name " + buildableResource.getFirst().getName() + " with instance ID " + instanceId);
        List<Instance> instances = volumeBuilderUtil.describeInstancesByInstanceIds(List.of(instanceId), auth);
        List<String> rootVolumeIds = volumeBuilderUtil.getRootVolumeIdsFromInstances(instances);
        List<CloudResource> rootVolumeResources = volumeBuilderUtil.updateRootVolumeResource(buildableResource, rootVolumeIds, auth);
        resourceNotifier.notifyUpdates(rootVolumeResources, auth.getCloudContext());
        return List.of();
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        // THIS SHOULD BE CALLED AFTER INSTANCE IS ALREADY DELETED WITH THE ROOT VOLUME ON CLOUD PROVIDER - ONLY DELETE REQUIRED IS FROM CBDB
        return resource;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_ROOT_DISK;
    }

    @Override
    public int order() {
        return LOWEST_PRECEDENCE;
    }
}
