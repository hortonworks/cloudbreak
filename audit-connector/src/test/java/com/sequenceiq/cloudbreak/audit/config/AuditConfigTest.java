package com.sequenceiq.cloudbreak.audit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.ReflectionUtils;

class AuditConfigTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][]{
                // testCaseName      endpoint                  valid  configured   host         port  exception
                { "null endpoint",   null,                     true,  false,       null,        null, null },
                { "blank endpoint",  "   ",                    true,  false,       null,        null, null },
                { "host only",       "hostname",               true,  true,        "hostname",  80,   null },
                { "host:port",       "hostname:1234",          true,  true,        "hostname",  1234, null },
                { "host:port:extra", "hostname:1234:whatelse", false, false,       null,        null, IllegalArgumentException.class },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void init(String testName, String endpoint, boolean valid, boolean configured, String host, Integer port, Class<? extends Exception> exceptionClass) {
        AuditConfig underTest = new AuditConfig();
        Field endpointField = ReflectionUtils.findField(AuditConfig.class, "endpoint");
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
