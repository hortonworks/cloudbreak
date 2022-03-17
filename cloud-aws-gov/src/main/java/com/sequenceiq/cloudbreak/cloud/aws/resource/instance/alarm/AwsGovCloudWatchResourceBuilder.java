package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsGovCloudWatchResourceBuilder extends AwsNativeCloudWatchResourceBuilder {

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant();
    }
}
