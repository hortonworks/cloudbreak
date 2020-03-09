package com.sequenceiq.distrox.v1.distrox.converter.cli.stack;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public interface StackRequestToCliRequestConverter {
    Object convert(StackV4Request source);

    CloudPlatform supportedPlatform();
}
