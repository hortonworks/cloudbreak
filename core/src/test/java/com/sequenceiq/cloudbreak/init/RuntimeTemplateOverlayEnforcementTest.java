package com.sequenceiq.cloudbreak.init;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayEnforcementTestUtil;

class RuntimeTemplateOverlayEnforcementTest {

    @ParameterizedTest(name = "{0}/<version> newer than the base must be an overlay, not a full on-disk directory")
    @CsvSource({
            "defaults/blueprints, .bp",
            "defaults/clustertemplates, .json"
    })
    void testIfNoFullOnDiskDirectoryAboveBase(String baseSubtree, String baseFileSuffix) throws IOException {
        RuntimeOverlayEnforcementTestUtil.testIfNoFullOnDiskDirectoryAboveBase(baseSubtree, baseFileSuffix);
    }
}
