package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE_RM;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class ArmConstants {

    public static final Platform AZURE_RM_PLATFORM = Platform.platform(AZURE_RM);

    public static final Variant AZURE_RM_VARIANT = Variant.variant(AZURE_RM);

    private ArmConstants() {
    }
}
