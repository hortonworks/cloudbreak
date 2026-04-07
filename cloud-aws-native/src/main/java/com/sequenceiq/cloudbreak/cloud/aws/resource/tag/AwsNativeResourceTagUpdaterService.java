package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Service responsible for updating tags on AWS native cloud resources.
 *
 * <p>This service uses strategy pattern to delegate tag update operations
 * to specific implementations of {@link AwsNativeResourceTagUpdateStrategy}.
 * Each strategy declares which {@link ResourceType}s it supports.
 *
 * <p>Tag update failures are propagated to the caller to fail the tag update flow.
 * Unsupported resource types are skipped.
 */

@Service
public class AwsNativeResourceTagUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeResourceTagUpdaterService.class);

    private final Map<ResourceType, TagUpdateStrategy> tagUpdateStrategyMap;

    @Inject
    public AwsNativeResourceTagUpdaterService(List<TagUpdateStrategy> tagUpdateStrategies) {
        this.tagUpdateStrategyMap = tagUpdateStrategies.stream()
                .flatMap(strategy -> strategy.supportedTypes().stream().map(type -> Map.entry(type, strategy)))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> userDefinedTags) {
        ResourceType resourceType = cloudResource.getType();
        TagUpdateStrategy strategy = tagUpdateStrategyMap.get(resourceType);

        if (strategy == null) {
            LOGGER.debug("No tag update strategy found for resource type: {}", resourceType);
            return;
        }

        strategy.updateTags(authenticatedContext, cloudResource, userDefinedTags);
    }
}
