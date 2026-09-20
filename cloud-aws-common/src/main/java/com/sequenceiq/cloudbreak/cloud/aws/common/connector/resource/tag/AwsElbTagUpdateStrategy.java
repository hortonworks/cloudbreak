package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER_LISTENER;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP;

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
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.AddTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RemoveTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;

@Service
public class AwsElbTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsElbTagUpdateStrategy.class);

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(ELASTIC_LOAD_BALANCER, ELASTIC_LOAD_BALANCER_LISTENER, ELASTIC_LOAD_BALANCER_TARGET_GROUP);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonElasticLoadBalancingClient elbClient = elbClient(authenticatedContext);
        String resourceArn = cloudResource.getReference();
        AwsTaggedResourceTagSupport.updateTags(this, LOGGER, "ELB resource", resourceArn, tags,
                () -> existingTags(elbClient, resourceArn),
                () -> elbClient.addTags(AddTagsRequest.builder()
                        .resourceArns(resourceArn)
                        .tags(awsTaggingService.prepareElasticLoadBalancingTags(tags))
                        .build()));
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        AmazonElasticLoadBalancingClient elbClient = elbClient(authenticatedContext);
        String resourceArn = cloudResource.getReference();
        AwsTaggedResourceTagSupport.deleteTags(this, LOGGER, "ELB resource", resourceArn, tagKeys,
                () -> existingTags(elbClient, resourceArn),
                () -> elbClient.removeTags(RemoveTagsRequest.builder()
                        .resourceArns(resourceArn)
                        .tagKeys(tagKeys)
                        .build()));
    }

    private AmazonElasticLoadBalancingClient elbClient(AuthenticatedContext authenticatedContext) {
        return commonAwsClient.createElasticLoadBalancingClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());
    }

    private Map<String, String> existingTags(AmazonElasticLoadBalancingClient elbClient, String resourceArn) {
        return elbClient.describeTags(DescribeTagsRequest.builder()
                        .resourceArns(resourceArn)
                        .build())
                .tagDescriptions().stream()
                .filter(td -> td.resourceArn().equals(resourceArn))
                .findFirst()
                .map(td -> td.tags().stream()
                        .collect(Collectors.toMap(Tag::key, Tag::value)))
                .orElse(Map.of());
    }
}
