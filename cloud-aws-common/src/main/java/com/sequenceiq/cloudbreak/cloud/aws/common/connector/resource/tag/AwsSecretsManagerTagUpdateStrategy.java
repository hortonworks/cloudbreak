package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_SECRETSMANAGER_SECRET;

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
import software.amazon.awssdk.services.secretsmanager.model.UntagResourceRequest;

@Service
public class AwsSecretsManagerTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSecretsManagerTagUpdateStrategy.class);

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
        AmazonSecretsManagerClient secretsManagerClient = secretsManagerClient(authenticatedContext);
        String secretId = cloudResource.getReference();
        AwsTaggedResourceTagSupport.updateTags(this, LOGGER, "Secrets Manager secret", secretId, tags,
                () -> existingTags(secretsManagerClient, secretId),
                () -> secretsManagerClient.tagResource(TagResourceRequest.builder()
                        .secretId(secretId)
                        .tags(awsTaggingService.prepareSecretsManagerTags(tags))
                        .build()));
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        AmazonSecretsManagerClient secretsManagerClient = secretsManagerClient(authenticatedContext);
        String secretId = cloudResource.getReference();
        AwsTaggedResourceTagSupport.deleteTags(this, LOGGER, "Secrets Manager secret", secretId, tagKeys,
                () -> existingTags(secretsManagerClient, secretId),
                () -> secretsManagerClient.untagResource(UntagResourceRequest.builder()
                        .secretId(secretId)
                        .tagKeys(tagKeys)
                        .build()));
    }

    private AmazonSecretsManagerClient secretsManagerClient(AuthenticatedContext authenticatedContext) {
        return commonAwsClient.createSecretsManagerClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());
    }

    private Map<String, String> existingTags(AmazonSecretsManagerClient secretsManagerClient, String secretId) {
        return secretsManagerClient.describeSecret(DescribeSecretRequest.builder()
                        .secretId(secretId)
                        .build())
                .tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));
    }
}
