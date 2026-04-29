package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
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
 * Service responsible for updating tags on Azure cloud resources.
 *
 * <p>This service uses strategy pattern to delegate tag update operations
 * to specific implementations of {@link TagUpdateStrategy}.
 * Each strategy declares which {@link ResourceType}s it supports.
 *
 * <p>Tag update failures are propagated to the caller to fail the tag update flow.
 * Unsupported resource types are skipped.
 */

@Service
public class AzureResourceTagUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceTagUpdaterService.class);

    private final Map<ResourceType, TagUpdateStrategy> tagUpdateStrategyMap;

    @Inject
    public AzureResourceTagUpdaterService(List<TagUpdateStrategy> tagUpdateStrategies) {
        this.tagUpdateStrategyMap = tagUpdateStrategies.stream()
                .flatMap(strategy -> strategy.supportedTypes().stream().map(type -> Map.entry(type, strategy)))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> userDefinedTags) throws IOException {
        ResourceType resourceType = cloudResource.getType();
        if (!resourceType.isTaggable()) {
            LOGGER.debug("Resource type {} is not taggable, skipping tag update.", resourceType);
            return;
        }

        TagUpdateStrategy strategy = tagUpdateStrategyMap.get(resourceType);
        if (strategy == null) {
            LOGGER.warn("Resource type {} is taggable but no tag update strategy is implemented.", resourceType);
            return;
        }

        strategy.updateTags(authenticatedContext, cloudResource, userDefinedTags);
    }
}
