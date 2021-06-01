package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsConstants implements CloudConstant {

    public static final Platform AWS_PLATFORM = Platform.platform(AWS);

    public static final Variant AWS_VARIANT = Variant.variant(AWS);

    private AwsConstants() {
    }

    @Override
    public Platform platform() {
        return AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AWS_VARIANT;
    }
}
