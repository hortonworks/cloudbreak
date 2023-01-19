package com.sequenceiq.cloudbreak.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RdsSslModeTest {

    static Object[][] sslModeDataProvider() {
        return new Object[][]{
                // testCaseName sslMode resultExpected
                {"sslMode=null", null, false},
                {"sslMode=DISABLED", RdsSslMode.DISABLED, false},
                {"sslMode=ENABLED", RdsSslMode.ENABLED, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslModeDataProvider")
    void isEnabledTest(String testCaseName, RdsSslMode sslMode, boolean resultExpected) {
        assertThat(RdsSslMode.isEnabled(sslMode)).isEqualTo(resultExpected);
    }

    static Object[][] sslEnforcementDataProvider() {
        return new Object[][]{
                // sslEnforcementEnabled, resultExpected
                {false, RdsSslMode.DISABLED},
                {true, RdsSslMode.ENABLED},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslEnforcementDataProvider")
    void fromBooleanTest(boolean sslEnforcementEnabled, RdsSslMode resultExpected) {
        assertThat(RdsSslMode.fromBoolean(sslEnforcementEnabled)).isEqualTo(resultExpected);
    }

}