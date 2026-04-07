package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_SECRETSMANAGER_SECRET;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecretsManagerClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.secretsmanager.model.Tag;
import software.amazon.awssdk.services.secretsmanager.model.TagResourceRequest;

@Service
public class SecretsManagerTagUpdateStrategy implements TagUpdateStrategy {

    @Inject
    private CommonAwsClient commonAwsClient;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AWS_SECRETSMANAGER_SECRET);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonSecretsManagerClient secretsManagerClient = commonAwsClient.createSecretsManagerClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());

        List<Tag> secretsTags = tags.entrySet().stream()
                .map(e -> software.amazon.awssdk.services.secretsmanager.model.Tag.builder()
                        .key(e.getKey())
                        .value(e.getValue())
                        .build())
                .toList();

        secretsManagerClient.tagResource(TagResourceRequest.builder()
                .secretId(cloudResource.getReference())
                .tags(secretsTags)
                .build());
    }
}
