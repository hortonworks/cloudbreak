package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_EFS;

import java.util.Collection;
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
        AmazonEfsClient efsClient = commonAwsClient.createElasticFileSystemClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());

        String resourceId = cloudResource.getReference();

        Map<String, String> existingTags = efsClient.listTagsForResource(
                        ListTagsForResourceRequest.builder()
                                .resourceId(resourceId)
                                .build())
                .tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));

        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.info("Tags for EFS resource {} are already up to date, skipping update.", resourceId);
            return;
        }

        Collection<Tag> efsTags = awsTaggingService.prepareEfsTags(tags);

        efsClient.tagResource(TagResourceRequest.builder()
                .resourceId(resourceId)
                .tags(efsTags)
                .build());
    }
}