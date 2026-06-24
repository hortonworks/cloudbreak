package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_VARIANT;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.StackDto;

@Service
public class AwsRepairSecurityGroupValidationTriggerService {

    public boolean shouldRunSecurityGroupValidation(StackDto stackDto) {
        if (!CloudPlatform.AWS.name().equals(stackDto.getCloudPlatform())) {
            return false;
        }
        String platformVariant = stackDto.getPlatformVariant();
        return AWS_VARIANT.variant().value().equals(platformVariant)
                || AWS_NATIVE_VARIANT.variant().value().equals(platformVariant)
                || AWS_NATIVE_GOV_VARIANT.variant().value().equals(platformVariant);
    }
}
