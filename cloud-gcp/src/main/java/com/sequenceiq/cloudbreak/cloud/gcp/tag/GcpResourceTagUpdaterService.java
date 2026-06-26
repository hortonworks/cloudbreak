package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.AbstractResourceTagUpdaterService;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Service responsible for updating tags (labels) on GCP cloud resources.
 *
 * <p>This service uses strategy pattern to delegate tag update operations
 * to specific implementations of {@link TagUpdateStrategy}.
 * Each strategy declares which {@link ResourceType}s it supports.
 *
 * <p>Tag update failures are propagated to the caller to fail the tag update flow.
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
}