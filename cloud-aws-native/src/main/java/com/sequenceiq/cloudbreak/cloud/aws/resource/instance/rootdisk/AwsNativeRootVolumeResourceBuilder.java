package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.rootdisk;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_ROOT_VOLUME_RESOURCE_BUILDER_ORDER;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsRootVolumeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsNativeRootVolumeResourceBuilder extends AwsRootVolumeResourceBuilder {

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant();
    }

    @Override
    public int order() {
        return NATIVE_ROOT_VOLUME_RESOURCE_BUILDER_ORDER;
    }
}
