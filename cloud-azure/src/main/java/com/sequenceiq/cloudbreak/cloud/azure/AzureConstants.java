package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@Service
public class AzureConstants implements CloudConstant {

    public static final String[] VARIANTS = new String[] {
            CloudConstants.AZURE,
    };

    public static final Platform PLATFORM = Platform.platform(AZURE);

    public static final Variant VARIANT = Variant.variant(AZURE);

    public static final int NOT_FOUND = 404;

    private AzureConstants() {

    }

    @Override
    public Platform platform() {
        return PLATFORM;
    }

    @Override
    public Variant variant() {
        return VARIANT;
    }

    @Override
    public String[] variants() {
        return VARIANTS;
    }
}
