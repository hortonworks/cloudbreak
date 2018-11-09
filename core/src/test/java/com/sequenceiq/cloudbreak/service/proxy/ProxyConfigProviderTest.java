package com.sequenceiq.cloudbreak.service.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@RunWith(MockitoJUnitRunner.class)
public class ProxyConfigProviderTest {

    @InjectMocks
    private ProxyConfigProvider proxyConfigProvider;

    private Cluster cluster;

    private Map<String, SaltPillarProperties> servicePillar;

    @Before
    public void init() {
        cluster = new Cluster();
        servicePillar = new HashMap<>();
    }

    @Test
    public void testNoProxyConfig() {
        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, cluster);
        assertTrue(servicePillar.isEmpty());
    }

    @Test
    public void testWithoutAuthProxy() {
        ProxyConfig proxyConfig = new ProxyConfig();
        Map<String, Object> properties = testProxyCore(proxyConfig);

        assertTrue(StringUtils.isNotBlank((CharSequence) properties.get("host")));
        assertTrue(StringUtils.isNotBlank((CharSequence) properties.get("protocol")));
        assertNotNull(properties.get("port"));
        assertFalse(properties.containsKey("user"));
        assertFalse(properties.containsKey("password"));
    }

    @Test
    public void testWithoutAuthUsernameSetProxy() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setUserName("test");
        Map<String, Object> properties = testProxyCore(proxyConfig);
        assertFalse(properties.containsKey("user"));
        assertFalse(properties.containsKey("password"));
    }

    @Test
    public void testWithoutAuthPasswordSetProxy() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setPassword("test");
        Map<String, Object> properties = testProxyCore(proxyConfig);
        assertFalse(properties.containsKey("user"));
        assertFalse(properties.containsKey("password"));
    }

    @Test
    public void testWithAuthProxy() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setUserName("user");
        proxyConfig.setPassword("pass");
        Map<String, Object> properties = testProxyCore(proxyConfig);
        assertTrue(StringUtils.isNotBlank((CharSequence) properties.get("user")));
        assertTrue(StringUtils.isNotBlank((CharSequence) properties.get("password")));
    }

    private Map<String, Object> testProxyCore(ProxyConfig proxyConfig) {
        proxyConfig.setServerHost("test");
        proxyConfig.setServerPort(3128);
        proxyConfig.setProtocol("http");
        cluster.setProxyConfig(proxyConfig);
        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, cluster);
        SaltPillarProperties pillarProperties = servicePillar.get(ProxyConfigProvider.PROXY_KEY);
        assertNotNull(pillarProperties);
        assertEquals(ProxyConfigProvider.PROXY_SLS_PATH, pillarProperties.getPath());
        return (Map<String, Object>) pillarProperties.getProperties().get(ProxyConfigProvider.PROXY_KEY);
    }

}