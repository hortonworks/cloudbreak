package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class AzureConstants {

    public static final Platform PLATFORM = Platform.platform(AZURE);

    public static final Variant VARIANT = Variant.variant(AZURE);

    public static final int NOT_FOUND = 404;

    private AzureConstants() {

    }
}
