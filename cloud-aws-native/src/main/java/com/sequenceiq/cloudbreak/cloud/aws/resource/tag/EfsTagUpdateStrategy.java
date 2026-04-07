package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_EFS;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.efs.model.Tag;
import software.amazon.awssdk.services.efs.model.TagResourceRequest;

@Service
public class EfsTagUpdateStrategy implements TagUpdateStrategy {

    @Inject
    private CommonAwsClient commonAwsClient;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AWS_EFS);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonEfsClient efsClient = commonAwsClient.createElasticFileSystemClient(new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());

        List<Tag> efsTags = tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .key(e.getKey())
                        .value(e.getValue())
                        .build())
                .toList();

        efsClient.tagResource(TagResourceRequest.builder()
                .resourceId(cloudResource.getReference())
                .tags(efsTags)
                .build());
    }
}
