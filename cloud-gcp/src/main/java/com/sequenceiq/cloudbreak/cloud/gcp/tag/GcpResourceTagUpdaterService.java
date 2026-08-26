package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.AbstractResourceTagUpdaterService;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Service responsible for updating and deleting tags (labels) on GCP cloud resources.
 *
 * <p>This service uses the strategy pattern to delegate tag update and delete operations
 * to specific implementations of {@link TagUpdateStrategy}.
 * Each strategy declares which {@link ResourceType}s it supports.
 *
 * <p>Tag keys and values are normalized for GCP labels via {@link #prepareTags(Map)}
 * and {@link #prepareTagKeys(Set)} before dispatch.
 *
 * <p>Tag update and delete failures are propagated to the caller to fail the tag flow.
 * Unsupported resource types are skipped.
 */

@Service
public class GcpResourceTagUpdaterService extends AbstractResourceTagUpdaterService {

    private final GcpLabelUtil gcpLabelUtil;

    @Inject
    public GcpResourceTagUpdaterService(List<TagUpdateStrategy> tagUpdateStrategies, GcpLabelUtil gcpLabelUtil) {
        super(tagUpdateStrategies);
        this.gcpLabelUtil = gcpLabelUtil;
    }

    @Override
    protected Map<String, String> prepareTags(Map<String, String> tags) {
        return gcpLabelUtil.createLabelsFromTagsMap(tags);
    }

    @Override
    protected Set<String> prepareTagKeys(Set<String> tagKeys) {
        return tagKeys.stream()
                .map(gcpLabelUtil::transformLabelKeyOrValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}