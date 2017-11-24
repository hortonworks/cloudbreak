package com.sequenceiq.cloudbreak.cloud.byos;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class BYOSConstants implements CloudConstant {

    public static final Platform BYOS_PLATFORM = Platform.platform(BYOS);

    public static final Variant BYOS_VARIANT = Variant.variant(BYOS);

    private BYOSConstants() {
    }

    @Override
    public Platform platform() {
        return BYOS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return BYOS_VARIANT;
    }
}