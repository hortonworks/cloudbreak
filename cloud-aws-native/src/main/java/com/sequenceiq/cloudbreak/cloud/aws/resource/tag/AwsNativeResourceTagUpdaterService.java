package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.AbstractResourceTagUpdaterService;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Service responsible for updating tags on AWS native cloud resources.
 *
 * <p>This service uses strategy pattern to delegate tag update operations
 * to specific implementations of {@link TagUpdateStrategy}.
 * Each strategy declares which {@link ResourceType}s it supports.
 *
 * <p>Tag update failures are propagated to the caller to fail the tag update flow.
 * Unsupported resource types are skipped.
 */

@Service
public class AwsNativeResourceTagUpdaterService extends AbstractResourceTagUpdaterService {

    @Inject
    public AwsNativeResourceTagUpdaterService(List<TagUpdateStrategy> tagUpdateStrategies) {
        super(tagUpdateStrategies);
    }

}

