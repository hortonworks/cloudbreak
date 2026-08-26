package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.tag;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.AbstractResourceTagUpdaterService;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Service responsible for updating and deleting tags on AWS CloudFormation cloud resources.
 *
 * <p>This service uses the strategy pattern to delegate tag update and delete operations
 * to specific implementations of {@link TagUpdateStrategy}.
 * Each strategy declares which {@link ResourceType}s it supports.
 *
 * <p>Tag update and delete failures are propagated to the caller to fail the tag flow.
 * Unsupported resource types are skipped.
 */

@Service
public class AwsResourceTagUpdaterService extends AbstractResourceTagUpdaterService {

    @Inject
    public AwsResourceTagUpdaterService(List<TagUpdateStrategy> tagUpdateStrategies) {
        super(tagUpdateStrategies);
    }
}