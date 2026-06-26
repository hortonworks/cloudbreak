package com.sequenceiq.cloudbreak.cloud;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractResourceTagUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResourceTagUpdaterService.class);

    private final Map<ResourceType, TagUpdateStrategy> tagUpdateStrategyMap;

    protected AbstractResourceTagUpdaterService(List<TagUpdateStrategy> tagUpdateStrategies) {
        this.tagUpdateStrategyMap = tagUpdateStrategies.stream()
                .flatMap(strategy -> strategy.supportedTypes().stream().map(type -> Map.entry(type, strategy)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void updateTags(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources, Map<String, String> tags) {
        List<CloudResource> taggableResources = cloudResources.stream()
                .filter(r -> r.getType().isTaggable())
                .toList();

        Map<Boolean, List<CloudResource>> strategyExistenceMap = taggableResources.stream()
                .collect(Collectors.partitioningBy(r -> tagUpdateStrategyMap.containsKey(r.getType())));

        strategyExistenceMap.getOrDefault(false, List.of())
                .forEach(r -> LOGGER.warn("Resource type {} is taggable but no tag update strategy is implemented.", r.getType()));

        Map<TagUpdateStrategy, List<CloudResource>> resourcesByStrategy = strategyExistenceMap.getOrDefault(true, List.of()).stream()
                .collect(Collectors.groupingBy(
                        r -> tagUpdateStrategyMap.get(r.getType()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        resourcesByStrategy.forEach((strategy, resources) -> {
            try {
                Map<String, String> preparedTags = prepareTags(tags);
                if (strategy.isBatchUpdateSupported()) {
                    LOGGER.info("Updating tags in batch for {} resources using {} with tags: {}",
                            resources.size(), strategy.getClass().getSimpleName(), preparedTags);
                    strategy.batchUpdateTags(authenticatedContext, resources, preparedTags);
                } else {
                    for (CloudResource cloudResource : resources) {
                        LOGGER.info("Updating tags for cloud resource: {} with type: {} with tags: {}",
                                cloudResource.getName(), cloudResource.getType(), preparedTags);
                        strategy.updateTags(authenticatedContext, cloudResource, preparedTags);
                    }
                }
            } catch (Exception e) {
                throw handleFailure(strategy, e);
            }
        });
    }

    protected Map<String, String> prepareTags(Map<String, String> tags) {
        return tags;
    }

    protected RuntimeException handleFailure(TagUpdateStrategy strategy, Exception e) {
        return new CloudConnectorException(String.format("Failed to update tags for strategy: %s", strategy.getClass().getSimpleName()), e);
    }
}