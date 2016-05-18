package com.sequenceiq.cloudbreak.cloud.mock;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

/**
 * Created by perdos on 4/22/16.
 */
public class MockConstants {

    public static final String MOCK = "MOCK";
    public static final Platform MOCK_PLATFORM = Platform.platform(MOCK);
    public static final Variant MOCK_VARIANT = Variant.variant(MOCK);

    private MockConstants() {
    }
}
