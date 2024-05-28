package com.sequenceiq.cloudbreak.cloud.aws.resource.kms;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.kms.AwsKmsKeyResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsNativeKmsKeyResourceBuilder extends AwsKmsKeyResourceBuilder {

    @Override
    public Variant variant() {
        return AWS_NATIVE_VARIANT.variant();
    }

}
