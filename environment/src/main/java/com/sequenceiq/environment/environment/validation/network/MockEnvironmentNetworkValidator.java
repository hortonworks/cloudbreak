package com.sequenceiq.environment.environment.validation.network;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class MockEnvironmentNetworkValidator implements EnvironmentNetworkValidator {
    @Override
    public void validate(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return MOCK;
    }

}
