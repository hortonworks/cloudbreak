package com.sequenceiq.cloudbreak.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class CloudbreakProxyConfigTest {

    private final CloudbreakProxyConfig proxyConfig = new CloudbreakProxyConfig();

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

}
