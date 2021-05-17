package com.sequenceiq.environment.environment.validation.network.yarn;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.YARN;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class YarnEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    @Override
    public void validateDuringFlow(EnvironmentValidationDto environmentValidationDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (networkDto != null) {
            if (networkDto.getYarn() != null) {
                String cloudPlatformName = getCloudPlatform().name();
                if (StringUtils.isEmpty(networkDto.getYarn().getQueue())) {
                    resultBuilder.error(missingParamErrorMessage("Queue(queue)", cloudPlatformName));
                }
                if (networkDto.getYarn().getLifetime() != null && networkDto.getYarn().getLifetime() < 0) {
                    resultBuilder.error(String.format("The 'lifetime' parameter should be non negative for '%s' environment specific network!",
                            cloudPlatformName));
                }
            } else {
                resultBuilder.error(missingParamsErrorMsg(YARN));
            }
        }
    }

    @Override
    public void validateDuringRequest(NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return YARN;
    }

}
