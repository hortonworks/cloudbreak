package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_KMS_KEY;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.kms.model.Tag;
import software.amazon.awssdk.services.kms.model.TagResourceRequest;

@Service
public class KmsTagUpdateStrategy implements TagUpdateStrategy {

    @Inject
    private CommonAwsClient commonAwsClient;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AWS_KMS_KEY);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonKmsClient kmsClient = commonAwsClient.createAWSKMS(new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());

        List<Tag> kmsTags = tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .tagKey(e.getKey())
                        .tagValue(e.getValue())
                        .build())
                .toList();

        kmsClient.tagResource(TagResourceRequest.builder()
                .keyId(cloudResource.getReference())
                .tags(kmsTags)
                .build());
    }
}
