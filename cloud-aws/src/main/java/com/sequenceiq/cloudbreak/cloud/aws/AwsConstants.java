package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class AwsConstants {

    public static final Platform AWS_PLATFORM = Platform.platform(AWS);
    public static final Variant AWS_VARIANT = Variant.variant(AWS);

    private AwsConstants() {
    }
}
