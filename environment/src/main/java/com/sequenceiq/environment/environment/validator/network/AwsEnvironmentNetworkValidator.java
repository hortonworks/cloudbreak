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
    public void validate(EnvironmentNetworkV1Request networkV1Request, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (networkV1Request != null) {
            if (networkV1Request.getAws() != null) {
                if (StringUtils.isEmpty(networkV1Request.getAws().getVpcId())) {
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
