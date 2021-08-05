package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.rootdisk;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_ROOT_DISK_TAGGING_RESOURCE_BUILDER_ORDER;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AWS_ROOT_DISK_TAGGING;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.AbstractAwsNativeComputeBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class RootDiskTaggingResourceBuilder extends AbstractAwsNativeComputeBuilder {

    @Inject
    private PersistenceRetriever persistenceRetriever;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        String resourceName = getResourceNameService().resourceName(resourceType(), context.getName(), auth.getCloudContext().getId(), group.getName(),
                privateId);
        return singletonList(CloudResource.builder()
                .group(group.getName())
                .type(resourceType())
                .status(CREATED)
                .name(resourceName)
                .persistent(true)
                .build());
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance instance, long privateId,
        AuthenticatedContext auth, Group group, List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        Optional<CloudResource> instanceResourceOpt = persistenceRetriever.notifyRetrieve(
                auth.getCloudContext().getId(),
                String.valueOf(privateId),
                CommonStatus.CREATED,
                ResourceType.AWS_INSTANCE);
        AmazonEc2Client amazonEc2Client = context.getAmazonEc2Client();
        awsTaggingService.tagRootVolumes(auth, amazonEc2Client, List.of(instanceResourceOpt.get()), cloudStack.getTags());
        return List.of();
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        return null;
    }

    @Override
    protected boolean isFinished(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        return true;
    }

    @Override
    public ResourceType resourceType() {
        return AWS_ROOT_DISK_TAGGING;
    }

    @Override
    public int order() {
        return NATIVE_ROOT_DISK_TAGGING_RESOURCE_BUILDER_ORDER;
    }
}
