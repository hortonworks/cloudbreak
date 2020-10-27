package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SslModeTest {

    static Object[][] sslDataProvider() {
        return new Object[][]{
                // testCaseName sslMode resultExpected
                {"sslMode=null", null, false},
                {"sslMode=DISABLED", SslMode.DISABLED, false},
                {"sslMode=ENABLED", SslMode.ENABLED, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslDataProvider")
    void isEnabledTest(String testCaseName, SslMode sslMode, boolean resultExpected) {
        assertThat(SslMode.isEnabled(sslMode)).isEqualTo(resultExpected);
    }

}