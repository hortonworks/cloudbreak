package com.sequenceiq.cloudbreak.cloud.aws.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_VOLUME_RESOURCE_BUILDER_ORDER;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsEfsResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsNativeEfsResourceBuilder extends AwsEfsResourceBuilder {

    @Override
    public Variant variant() {
        return AWS_NATIVE_VARIANT.variant();
    }

    @Override
    public int order() {
        return NATIVE_VOLUME_RESOURCE_BUILDER_ORDER;
    }
}
