package com.sequenceiq.environment.environment.validation.network;

import static com.sequenceiq.environment.CloudPlatform.YARN;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class YarnEnvironmentNetworkValidator implements EnvironmentNetworkValidator {
    @Override
    public void validate(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (networkDto != null) {
            if (networkDto.getYarn() != null) {
                if (StringUtils.isEmpty(networkDto.getYarn().getQueue())) {
                    resultBuilder.error(missingParamErrorMessage("Queue(queue)", getCloudPlatform().name()));
                }
            } else {
                resultBuilder.error(missingParamsErrorMsg(YARN));
            }
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return YARN;
    }

}
