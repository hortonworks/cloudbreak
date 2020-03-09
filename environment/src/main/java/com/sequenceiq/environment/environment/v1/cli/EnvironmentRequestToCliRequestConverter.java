package com.sequenceiq.environment.environment.v1.cli;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;

public interface EnvironmentRequestToCliRequestConverter {
    Object convert(EnvironmentRequest source);

    CloudPlatform supportedPlatform();
}
