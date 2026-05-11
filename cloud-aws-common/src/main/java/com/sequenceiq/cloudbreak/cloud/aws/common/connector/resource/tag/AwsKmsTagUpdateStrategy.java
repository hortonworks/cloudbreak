package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_KMS_KEY;

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
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.Tag;
import software.amazon.awssdk.services.kms.model.TagResourceRequest;

@Service
public class AwsKmsTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsKmsTagUpdateStrategy.class);

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AWS_KMS_KEY);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonKmsClient kmsClient = commonAwsClient.createAWSKMS(
                new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());

        String keyId = cloudResource.getReference();

        Map<String, String> existingTags = kmsClient.listResourceTags(
                        ListResourceTagsRequest.builder()
                                .keyId(keyId)
                                .build())
                .tags().stream()
                .collect(Collectors.toMap(Tag::tagKey, Tag::tagValue));

        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.info("Tags for KMS key {} are already up to date, skipping update.", keyId);
            return;
        }

        Collection<Tag> kmsTags = awsTaggingService.prepareKmsTags(tags);

        kmsClient.tagResource(TagResourceRequest.builder()
                .keyId(keyId)
                .tags(kmsTags)
                .build());
    }
}