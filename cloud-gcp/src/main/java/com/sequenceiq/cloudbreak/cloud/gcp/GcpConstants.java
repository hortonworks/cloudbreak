package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@Service
public class GcpConstants implements CloudConstant {

    public static final String[] VARIANTS = new String[] {
            CloudConstants.GCP,
    };

    public static final Platform GCP_PLATFORM = Platform.platform(GCP);

    public static final Variant GCP_VARIANT = Variant.variant(GCP);

    public GcpConstants() {
    }

    @Override
    public Platform platform() {
        return GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GCP_VARIANT;
    }

    @Override
    public String[] variants() {
        return VARIANTS;
    }
}
