package com.sequenceiq.cloudbreak.cloud.cumulus.yarn;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.CUMULUS_YARN;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class CumulusYarnConstants implements CloudConstant {
    public static final Platform CUMULUS_YARN_PLATFORM = Platform.platform(CUMULUS_YARN);

    public static final Variant CUMULUS_YARN_VARIANT = Variant.variant(CUMULUS_YARN);

    public static final String CUMULUS_YARN_QUEUE_PARAMETER = "cumulusYarnQueue";

    public static final String CUMULUS_YARN_LIFETIME_PARAMETER = "lifeTime";

    public static final String CUMULUS_YARN_ENDPOINT_PARAMETER = "cumulusYarnEndpoint";

    private CumulusYarnConstants() {
    }

    @Override
    public Platform platform() {
        return CUMULUS_YARN_PLATFORM;
    }

    @Override
    public Variant variant() {
        return CUMULUS_YARN_VARIANT;
    }
}
