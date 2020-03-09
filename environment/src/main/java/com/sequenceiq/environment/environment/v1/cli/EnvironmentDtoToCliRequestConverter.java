package com.sequenceiq.environment.environment.v1.cli;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;

public interface EnvironmentDtoToCliRequestConverter {
    Object convert(EnvironmentDto source);

    CloudPlatform supportedPlatform();
}
