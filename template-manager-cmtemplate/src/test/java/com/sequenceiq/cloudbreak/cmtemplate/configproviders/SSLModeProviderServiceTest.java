package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SSLModeProviderServiceTest {

    @ParameterizedTest(name = "connectionString={0}")
    @ValueSource(strings = {"verify-ca", "verify-full"})
    void testGetSslModeBasedOnConnectionString(String connectionString) {
        assertEquals("sslmode=" + connectionString, SSLModeProviderService.getSslModeBasedOnConnectionString(connectionString));
    }
}
