package com.sequenceiq.datalake.configuration.overlay;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayEnforcementTestUtil;

class RuntimeDutyOverlayEnforcementTest {

    @Test
    void testIfNoFullOnDiskDirectoryAboveBase() throws IOException {
        RuntimeOverlayEnforcementTestUtil.testIfNoFullOnDiskDirectoryAboveBase("duties", ".json");
    }
}
