package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Service responsible for updating tags (labels) on GCP cloud resources.
 *
 * <p>This service uses strategy pattern to delegate tag update operations
 * to specific implementations of {@link GcpResourceTagUpdateStrategy}.
 * Each strategy declares which {@link ResourceType}s it supports.
 *
 * <p>Tag update failures are propagated to the caller to fail the tag update flow.
 * Unsupported resource types are skipped.
 */

@Service
public class GcpResourceTagUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceTagUpdaterService.class);

    private final GcpLabelUtil gcpLabelUtil;

    private final Map<ResourceType, GcpResourceTagUpdateStrategy> tagUpdateStrategyMap;

    @Inject
    public GcpResourceTagUpdaterService(List<GcpResourceTagUpdateStrategy> tagUpdateStrategies, GcpLabelUtil gcpLabelUtil) {
        this.gcpLabelUtil = gcpLabelUtil;
        this.tagUpdateStrategyMap = tagUpdateStrategies.stream()
                .flatMap(strategy -> strategy.supportedTypes().stream().map(type -> Map.entry(type, strategy)))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource,
            Map<String, String> userDefinedTags) throws IOException {
        ResourceType resourceType = cloudResource.getType();
        GcpResourceTagUpdateStrategy strategy = tagUpdateStrategyMap.get(resourceType);

        if (strategy == null) {
            LOGGER.warn("No tag update strategy found for resource type: {}", resourceType);
            return;
        }

        Map<String, String> userDefinedLabels = gcpLabelUtil.createLabelsFromTagsMap(userDefinedTags);
        strategy.updateTags(authenticatedContext, cloudResource, userDefinedLabels);
    }
}
