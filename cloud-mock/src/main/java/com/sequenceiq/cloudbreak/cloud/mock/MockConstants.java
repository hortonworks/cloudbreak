package com.sequenceiq.cloudbreak.cloud.mock;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@Service
class MockConstants implements CloudConstant {
    public static final String MOCK_ENDPOINT_PARAMETER = "mockEndpoint";

    public static final String[] VARIANTS = new String[] {
            CloudConstants.MOCK,
    };

    static final String MOCK = "MOCK";

    static final Platform MOCK_PLATFORM = Platform.platform(MOCK);

    static final Variant MOCK_VARIANT = Variant.variant(MOCK);

    private MockConstants() {
    }

    @Override
    public Platform platform() {
        return MOCK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return MOCK_VARIANT;
    }

    @Override
    public String[] variants() {
        return VARIANTS;
    }
}
