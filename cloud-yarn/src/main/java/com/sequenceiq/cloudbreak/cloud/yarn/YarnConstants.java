package com.sequenceiq.cloudbreak.cloud.yarn;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.YARN;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class YarnConstants implements CloudConstant {
    public static final Platform YARN_PLATFORM = Platform.platform(YARN);

    public static final Variant YARN_VARIANT = Variant.variant(YARN);

    public static final String YARN_ENDPOINT_PARAMETER = "yarnEndpoint";

    public static final String YARN_QUEUE_PARAMETER = "yarnQueue";

    public static final String YARN_LIFETIME_PARAMETER = "lifeTime";

    private YarnConstants() {
    }

    @Override
    public Platform platform() {
        return YarnConstants.YARN_PLATFORM;
    }

    @Override
    public Variant variant() {
        return YarnConstants.YARN_VARIANT;
    }
}
