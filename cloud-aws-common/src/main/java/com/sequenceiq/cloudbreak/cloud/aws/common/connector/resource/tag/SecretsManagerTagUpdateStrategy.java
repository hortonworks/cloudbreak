package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_SECRETSMANAGER_SECRET;

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
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecretsManagerClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.Tag;
import software.amazon.awssdk.services.secretsmanager.model.TagResourceRequest;

@Service
public class SecretsManagerTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretsManagerTagUpdateStrategy.class);

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AWS_SECRETSMANAGER_SECRET);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonSecretsManagerClient secretsManagerClient = commonAwsClient.createSecretsManagerClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());

        String secretId = cloudResource.getReference();

        Map<String, String> existingTags = secretsManagerClient.describeSecret(
                        DescribeSecretRequest.builder()
                                .secretId(secretId)
                                .build())
                .tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));

        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.info("Tags for Secrets Manager secret {} are already up to date, skipping update.", secretId);
            return;
        }

        Collection<Tag> secretsManagerTags = awsTaggingService.prepareSecretsManagerTags(tags);

        secretsManagerClient.tagResource(TagResourceRequest.builder()
                .secretId(secretId)
                .tags(secretsManagerTags)
                .build());
    }
}