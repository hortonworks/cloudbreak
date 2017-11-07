package com.sequenceiq.cloudbreak.cloud.yarn;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.YARN;

public class YarnConstants {
    public static final Platform YARN_PLATFORM = Platform.platform(YARN);

    public static final Variant YARN_VARIANT = Variant.variant(YARN);

    private YarnConstants() {
    }

}
