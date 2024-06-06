package com.sequenceiq.freeipa.service.encryption;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsNativeCloudInformationDecorator extends AwsCloudInformationDecorator {

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant();
    }

}
