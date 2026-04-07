package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER_LISTENER;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.AddTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;

@Service
public class ElbTagUpdateStrategy implements TagUpdateStrategy {

    @Inject
    private CommonAwsClient commonAwsClient;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(ELASTIC_LOAD_BALANCER, ELASTIC_LOAD_BALANCER_LISTENER, ELASTIC_LOAD_BALANCER_TARGET_GROUP);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonElasticLoadBalancingClient elbClient = commonAwsClient.createElasticLoadBalancingClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());

        List<Tag> elbTags = tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .key(e.getKey())
                        .value(e.getValue())
                        .build())
                .toList();

        elbClient.addTags(AddTagsRequest.builder()
                .resourceArns(cloudResource.getReference())
                .tags(elbTags)
                .build());
    }
}
