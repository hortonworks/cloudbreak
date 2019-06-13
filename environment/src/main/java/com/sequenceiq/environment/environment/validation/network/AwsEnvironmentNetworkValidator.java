package com.sequenceiq.environment.environment.validation.network;

import static com.sequenceiq.environment.CloudPlatform.AWS;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AwsEnvironmentNetworkValidator implements EnvironmentNetworkValidator {
    @Override
    public void validate(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (networkDto != null) {
            if (networkDto.getAws() != null) {
                if (StringUtils.isEmpty(networkDto.getAws().getVpcId())) {
                    resultBuilder.error(missingParamErrorMessage("VPC identifier(vpcId)", getCloudPlatform().name()));
                }
            } else {
                resultBuilder.error(missingParamsErrorMsg(AWS));
            }
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }

}
