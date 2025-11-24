package com.sequenceiq.cloudbreak.cloud.aws.common.providersync;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsNativeVolumeSetSyncer extends AwsVolumeSetSyncer {

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant();
    }
}
