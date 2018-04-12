package com.sequenceiq.cloudbreak.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationProxyConfigTest {

    private final ApplicationProxyConfig proxyConfig = new ApplicationProxyConfig();

    @Test
    public void testIsUseProxyForClusterConnectionWhenProxyProvided() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertTrue(useProxyForClusterConnection);
    }

    @Test
    public void testIsUseProxyForClusterConnectionWhenProxyProvidedButClusterInSameVnet() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", false);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    public void testIsUseProxyForClusterConnectionWhenProxyHostIsMissing() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    public void testIsUseProxyForClusterConnectionWhenProxyPortIsMissing() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    public void testIsUseProxyForClusterConnectionWhenProxyHostIsEmpty() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    public void testIsUseProxyForClusterConnectionWhenProxyPortIsEmpty() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "");
        ReflectionTestUtils.setField(proxyConfig, "useProxyForClusterConnection", true);

        boolean useProxyForClusterConnection = proxyConfig.isUseProxyForClusterConnection();

        assertFalse(useProxyForClusterConnection);
    }

    @Test
    public void testIsProxyAuthRequired() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyUser", "user");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPassword", "pass");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertTrue(authRequired);
    }

    @Test
    public void testIsProxyAuthRequiredWhenUserIsNull() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPassword", "pass");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertFalse(authRequired);
    }

    @Test
    public void testIsProxyAuthRequiredWhenPasswordIsNull() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyUser", "user");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertFalse(authRequired);
    }

    @Test
    public void testIsProxyAuthRequiredWhenProxyHostIsMissing() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "3128");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyUser", "user");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPassword", "pass");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertFalse(authRequired);
    }

    @Test
    public void testIsProxyAuthRequiredWhenProxyPortIsMissing() {
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyHost", "10.0.0.2");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPort", "");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyUser", "user");
        ReflectionTestUtils.setField(proxyConfig, "httpsProxyPassword", "pass");

        boolean authRequired = proxyConfig.isProxyAuthRequired();

        assertFalse(authRequired);
    }

}
