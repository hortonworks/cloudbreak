package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class GcpConstants {

    public static final Platform GCP_PLATFORM = Platform.platform(GCP);
    public static final Variant GCP_VARIANT = Variant.variant(GCP);

    private GcpConstants() {
    }
}
