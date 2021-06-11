package com.sequenceiq.cloudbreak.cloud.aws.resource;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractAwsBaseResourceChecker {

    protected CloudResource createNamedResource(ResourceType type, String name) {
        return new CloudResource.Builder().type(type).name(name).build();
    }
}
