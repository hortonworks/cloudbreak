package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.RDS_DB_PARAMETER_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.RDS_DB_SUBNET_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.RDS_INSTANCE;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * The AWS database cloud resources are created as a CloudFormation stack so the {@link AwsCloudFormationTagUpdateStrategy} already
 * handles the tag update of those resources. Nevertheless, I did not delete this class because each supported RDS resource type is
 * marked as taggable and their tag update should be handled here.
 */
@Service
public class AwsRdsTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsRdsTagUpdateStrategy.class);

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(RDS_INSTANCE, RDS_DB_SUBNET_GROUP, RDS_DB_PARAMETER_GROUP);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        LOGGER.debug("Cloud resource {} with type {} is taggable but tag update is already handled by the AwsCloudFormationTagUpdateStrategy.",
                cloudResource.getName(), cloudResource.getType());
    }
}
