package com.sequenceiq.cloudbreak.audit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.structuredevent.conf.StructuredEventEnablementConfig;

@ExtendWith(MockitoExtension.class)
class AuditConfigTest {

    @Mock
    private StructuredEventEnablementConfig structuredEventEnablementConfig;

    @InjectMocks
    private AuditConfig underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][]{
                // testCaseName      endpoint                            valid  enabled  configured   host         port  exception
                { "null endpoint, disabled",   null,                     true,  false,   false,       null,        null, null },
                { "blank endpoint, disabled",  "   ",                    true,  false,   false,       null,        null, null },
                { "null endpoint, enabled",    null,                     false, true,    false,       null,        null, IllegalStateException.class },
                { "blank endpoint, enabled",   "   ",                    false, true,    false,       null,        null, IllegalStateException.class },
                { "host only",                 "hostname",               true,  true,    true,        "hostname",  8982, null },
                { "host:port",                 "hostname:1234",          true,  true,    true,        "hostname",  1234, null },
                { "host:port:extra",           "hostname:1234:whatelse", false, true,    false,       null,        null, IllegalArgumentException.class },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    @MockitoSettings(strictness = Strictness.LENIENT)
    void init(String testName, String endpoint, boolean valid, boolean enabled, boolean configured,
            String host, Integer port, Class<? extends Exception> exceptionClass) {

        Field endpointField = ReflectionUtils.findField(AuditConfig.class, "endpoint");
        ReflectionUtils.makeAccessible(endpointField);
        ReflectionUtils.setField(endpointField, underTest, endpoint);
        when(structuredEventEnablementConfig.isAuditServiceEnabled()).thenReturn(enabled);

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
