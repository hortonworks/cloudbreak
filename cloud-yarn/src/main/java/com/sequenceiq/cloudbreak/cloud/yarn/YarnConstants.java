package com.sequenceiq.cloudbreak.cloud.yarn;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.YARN;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@Service
public class YarnConstants implements CloudConstant {

    public static final String[] VARIANTS = new String[] {
            CloudConstants.YARN,
    };

    public static final Platform YARN_PLATFORM = Platform.platform(YARN);

    public static final Variant YARN_VARIANT = Variant.variant(YARN);

    public static final String YARN_ENDPOINT_PARAMETER = "endpoint";

    public static final String YARN_QUEUE_PARAMETER = "yarnQueue";

    public static final String YARN_LIFETIME_PARAMETER = "lifeTime";

    private YarnConstants() {
    }

    @Override
    public Platform platform() {
        return YARN_PLATFORM;
    }

    @Override
    public Variant variant() {
        return YARN_VARIANT;
    }

    @Override
    public String[] variants() {
        return VARIANTS;
    }
}
