package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.RDS_DB_PARAMETER_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.RDS_DB_SUBNET_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.RDS_INSTANCE;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.rds.model.AddTagsToResourceRequest;
import software.amazon.awssdk.services.rds.model.Tag;

@Service
public class RdsTagUpdateStrategy implements TagUpdateStrategy {

    @Inject
    private CommonAwsClient commonAwsClient;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(RDS_INSTANCE, RDS_DB_SUBNET_GROUP, RDS_DB_PARAMETER_GROUP);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonRdsClient rdsClient = commonAwsClient.createRdsClient(authenticatedContext);

        List<Tag> rdsTags = tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .key(e.getKey())
                        .value(e.getValue())
                        .build())
                .toList();

        rdsClient.addTagsToResource(AddTagsToResourceRequest.builder()
                .resourceName(cloudResource.getReference())
                .tags(rdsTags)
                .build());
    }
}
