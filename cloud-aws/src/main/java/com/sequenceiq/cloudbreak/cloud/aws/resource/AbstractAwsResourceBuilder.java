package com.sequenceiq.cloudbreak.cloud.aws.resource;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.aws.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

public abstract class AbstractAwsResourceBuilder implements CloudPlatformAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAwsResourceBuilder.class);

    @Inject
    private AwsResourceNameService resourceNameService;

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_VARIANT;
    }

    public AwsResourceNameService getResourceNameService() {
        return resourceNameService;
    }

    protected List<CloudResourceStatus> checkResources(ResourceType type, AwsContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        return List.of();
    }
}
