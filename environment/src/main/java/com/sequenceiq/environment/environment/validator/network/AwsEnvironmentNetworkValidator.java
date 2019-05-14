package com.sequenceiq.environment.environment.validator.network;


import static com.sequenceiq.environment.CloudPlatform.AWS;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentNetworkV1Request;

@Component
public class AwsEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    @Override
    public void validate(EnvironmentNetworkV1Request networkRequest, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (networkRequest != null) {
            if (networkRequest.getAws() != null) {
                if (StringUtils.isEmpty(networkRequest.getAws().getVpcId())) {
                    resultBuilder.error(missingParamErrorMessage("VPC identifier(vpcId)'", getCloudPlatform().name()));
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
