package com.sequenceiq.cloudbreak.datalakedr.config;

import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class DatalakeDrConfigTest {

    @InjectMocks
    private DatalakeDrConfig underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][]{
            // testCaseName      endpoint                  valid  configured   host         port  exception
            { "null endpoint",   null,                     false, false,       null,        null, IllegalStateException.class },
            { "blank endpoint",  "   ",                    false, false,       null,        null, IllegalStateException.class },
            { "host only",       "hostname",               true,  true,        "hostname",  80,   null },
            { "host:port",       "hostname:1234",          true,  true,        "hostname",  1234, null },
            { "host:port:extra", "hostname:1234:whatelse", false, false,       null,        null, IllegalArgumentException.class },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    @MockitoSettings(strictness = Strictness.LENIENT)
    void init(String testName, String endpoint, boolean valid, boolean configured, String host,
              Integer port, Class<? extends Exception> exceptionClass) {

        Field endpointField = ReflectionUtils.findField(DatalakeDrConfig.class, "endpoint");
        ReflectionUtils.makeAccessible(endpointField);
        ReflectionUtils.setField(endpointField, underTest, endpoint);

        if (valid) {
            underTest.init();
            if (configured) {
                assertThat(underTest.isConfigured()).isTrue();
                assertThat(underTest.getHost()).isEqualTo(host);
                assertThat(underTest.getPort()).isEqualTo(port);
            } else {
                assertThat(underTest.isConfigured()).isFalse();
            }
        } else {
            assertThatThrownBy(underTest::init).isInstanceOf(exceptionClass);
        }
    }
}
