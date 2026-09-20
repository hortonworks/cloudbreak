package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_EFS;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.efs.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.efs.model.Tag;
import software.amazon.awssdk.services.efs.model.TagResourceRequest;
import software.amazon.awssdk.services.efs.model.UntagResourceRequest;

@Service
public class AwsEfsTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEfsTagUpdateStrategy.class);

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AWS_EFS);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonEfsClient efsClient = efsClient(authenticatedContext);
        String resourceId = cloudResource.getReference();
        AwsTaggedResourceTagSupport.updateTags(this, LOGGER, "EFS resource", resourceId, tags,
                () -> existingTags(efsClient, resourceId),
                () -> efsClient.tagResource(TagResourceRequest.builder()
                        .resourceId(resourceId)
                        .tags(awsTaggingService.prepareEfsTags(tags))
                        .build()));
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        AmazonEfsClient efsClient = efsClient(authenticatedContext);
        String resourceId = cloudResource.getReference();
        AwsTaggedResourceTagSupport.deleteTags(this, LOGGER, "EFS resource", resourceId, tagKeys,
                () -> existingTags(efsClient, resourceId),
                () -> efsClient.untagResource(UntagResourceRequest.builder()
                        .resourceId(resourceId)
                        .tagKeys(tagKeys)
                        .build()));
    }

    private AmazonEfsClient efsClient(AuthenticatedContext authenticatedContext) {
        return commonAwsClient.createElasticFileSystemClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());
    }

    private Map<String, String> existingTags(AmazonEfsClient efsClient, String resourceId) {
        return efsClient.listTagsForResource(ListTagsForResourceRequest.builder()
                        .resourceId(resourceId)
                        .build())
                .tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));
    }
}
