package com.sequenceiq.cloudbreak.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RdsSslModeTest {

    static Object[][] sslDataProvider() {
        return new Object[][]{
                // testCaseName sslMode resultExpected
                {"sslMode=null", null, false},
                {"sslMode=DISABLED", RdsSslMode.DISABLED, false},
                {"sslMode=ENABLED", RdsSslMode.ENABLED, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslDataProvider")
    void isEnabledTest(String testCaseName, RdsSslMode sslMode, boolean resultExpected) {
        assertThat(RdsSslMode.isEnabled(sslMode)).isEqualTo(resultExpected);
    }

}