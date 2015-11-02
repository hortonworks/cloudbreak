package com.sequenceiq.cloudbreak.cloud.gcp;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

public class GcpConstants {

    public static final Platform GCP_PLATFORM = Platform.platform(CloudPlatform.GCP.name());
    public static final Variant GCP_VARIANT = Variant.variant(CloudPlatform.GCP.name());

    private GcpConstants() {
    }
}
