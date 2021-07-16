package com.sequenceiq.cloudbreak.cloud.aws.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_VOLUME_RESOURCE_BUILDER_ORDER;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsNativeVolumeResourceBuilder extends AwsVolumeResourceBuilder {

    @Override
    public Variant variant() {
        return AWS_NATIVE_VARIANT.variant();
    }

    @Override
    protected String getSubnetId(AwsContext context, CloudInstance cloudInstance) {
        return cloudInstance.getSubnetId();
    }

    protected Optional<String> getAvailabilityZone(AwsContext context, CloudInstance cloudInstance) {
        return Optional.ofNullable(cloudInstance.getAvailabilityZone());
    }

    @Override
    public int order() {
        return NATIVE_VOLUME_RESOURCE_BUILDER_ORDER;
    }
}
