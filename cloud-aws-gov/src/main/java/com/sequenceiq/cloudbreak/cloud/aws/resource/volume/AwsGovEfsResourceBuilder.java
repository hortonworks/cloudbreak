package com.sequenceiq.cloudbreak.cloud.aws.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsGovEfsResourceBuilder extends AwsNativeEfsResourceBuilder {

    @Override
    public Variant variant() {
        return AWS_NATIVE_GOV_VARIANT.variant();
    }
}
