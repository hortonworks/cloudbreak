package com.sequenceiq.cloudbreak.cloud.mock;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

class MockConstants {

    static final String MOCK = "MOCK";
    static final Platform MOCK_PLATFORM = Platform.platform(MOCK);
    static final Variant MOCK_VARIANT = Variant.variant(MOCK);

    private MockConstants() {
    }
}
