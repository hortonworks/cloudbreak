package com.sequenceiq.cloudbreak.proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApplicationProxyConfigTest {

    private final ApplicationProxyConfig proxyConfig = new ApplicationProxyConfig();

    @Test
    void testIsUseProxyForClusterConnectionWhenProxyProvided() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertTrue(useProxyForClusterConnection);
    }

    @Test
    void testIsUseProxyForClusterConnectionWhenProxyProvidedButClusterInSameVnet() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", false);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    void testIsUseProxyForClusterConnectionWhenProxyHostIsMissing() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    void testIsUseProxyForClusterConnectionWhenProxyPortIsMissing() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    void testIsUseProxyForClusterConnectionWhenProxyHostIsEmpty() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    void testIsUseProxyForClusterConnectionWhenProxyPortIsEmpty() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    void testIsProxyAuthRequired() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyUser", "user");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPassword", "pass");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertTrue(authRequired);
    }

    @Test
    void testIsProxyAuthRequiredWhenUserIsNull() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPassword", "pass");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertFalse(authRequired);
    }

    @Test
    void testIsProxyAuthRequiredWhenPasswordIsNull() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyUser", "user");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertFalse(authRequired);
    }

    @Test
    void testIsProxyAuthRequiredWhenProxyHostIsMissing() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyUser", "user");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPassword", "pass");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertFalse(authRequired);
    }

    @Test
    void testIsProxyAuthRequiredWhenProxyPortIsMissing() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyUser", "user");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPassword", "pass");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertFalse(authRequired);
    }

}
