package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER_LISTENER;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP;

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
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.AddTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;

@Service
public class ElbTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElbTagUpdateStrategy.class);

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
        AmazonElasticLoadBalancingClient elbClient = commonAwsClient.createElasticLoadBalancingClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());

        String resourceArn = cloudResource.getReference();

        Map<String, String> existingTags = elbClient.describeTags(
                        DescribeTagsRequest.builder()
                                .resourceArns(resourceArn)
                                .build())
                .tagDescriptions().stream()
                .filter(td -> td.resourceArn().equals(resourceArn))
                .findFirst()
                .map(td -> td.tags().stream()
                        .collect(Collectors.toMap(Tag::key, Tag::value)))
                .orElse(Map.of());

        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.info("Tags for ELB resource {} are already up to date, skipping update.", resourceArn);
            return;
        }

        Collection<Tag> elbTags = awsTaggingService.prepareElasticLoadBalancingTags(tags);

        elbClient.addTags(AddTagsRequest.builder()
                .resourceArns(resourceArn)
                .tags(elbTags)
                .build());
    }
}