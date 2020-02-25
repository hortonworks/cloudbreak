package com.sequenceiq.environment.environment.validation.network;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class MockEnvironmentNetworkValidator implements EnvironmentNetworkValidator {
    @Override
    public void validateDuringFlow(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
    }

    @Override
    public void validateDuringRequest(
            NetworkDto networkV1Request, Map<String, CloudSubnet> subnetMetas, ValidationResult.ValidationResultBuilder resultBuilder) {
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return MOCK;
    }

}
