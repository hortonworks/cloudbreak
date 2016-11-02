package com.sequenceiq.cloudbreak.cloud.byos;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class BYOSConstants {

    public static final Platform BYOS_PLATFORM = Platform.platform(BYOS);

    public static final Variant BYOS_VARIANT = Variant.variant(BYOS);

    private BYOSConstants() {
    }
}