package com.sequenceiq.cloudbreak.common.runtime.overlay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class RuntimeOverlayEnforcementTestUtilTest {

    @Test
    void passesWhenOnlyTheBaseIsOnDisk() {
        assertThatCode(() -> RuntimeOverlayEnforcementTestUtil.testIfNoFullOnDiskDirectoryAboveBase("7.0.0", "widgets", ".json"))
                .doesNotThrowAnyException();
    }

    @Test
    void passesForVersionsBelowTheBase() {
        assertThatCode(() -> RuntimeOverlayEnforcementTestUtil.testIfNoFullOnDiskDirectoryAboveBase("7.0.1", "widgets", ".json"))
                .doesNotThrowAnyException();
    }

    @Test
    void failsWhenAVersionAboveTheBaseIsOnDisk() {
        AssertionFailedError error = assertThrows(AssertionFailedError.class,
                () -> RuntimeOverlayEnforcementTestUtil.testIfNoFullOnDiskDirectoryAboveBase("6.9.9", "widgets", ".json"));

        assertThat(error.getMessage()).contains("[7.0.0]").contains("newer than the base 6.9.9");
    }

    @Test
    void onlyScansFilesWithTheBaseSuffix() {
        assertThatCode(() -> RuntimeOverlayEnforcementTestUtil.testIfNoFullOnDiskDirectoryAboveBase("6.9.9", "gadgets", ".json"))
                .doesNotThrowAnyException();
    }

    @Test
    void readsTheBaseFromTheSharedConfig() throws Exception {
        assertThat(RuntimeOverlayEnforcementTestUtil.configuredBaseVersion()).matches("\\d+(\\.\\d+)+");
    }
}
