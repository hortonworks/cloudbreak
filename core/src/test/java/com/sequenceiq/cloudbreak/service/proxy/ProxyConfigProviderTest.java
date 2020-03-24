package com.sequenceiq.cloudbreak.service.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.ProxyConfig.ProxyConfigBuilder;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class ProxyConfigProviderTest {

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Workspace workspace;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Stack stack;

    @InjectMocks
    private ProxyConfigProvider proxyConfigProvider;

    private Cluster cluster;

    private Map<String, SaltPillarProperties> servicePillar;

    @Before
    public void init() {
        cluster = new Cluster();
        servicePillar = new HashMap<>();
        cluster.setWorkspace(workspace);
        cluster.setStack(stack);
        when(workspace.getTenant().getName()).thenReturn("tenantId");
        when(stack.getCreator().getUserCrn()).thenReturn("aUserCrn");
    }

    @Test
    public void testNoProxyConfig() {
        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, cluster);
        assertTrue(servicePillar.isEmpty());
    }

    @Test
    public void testWithoutAuthProxy() {
        ProxyConfigBuilder proxyConfig = ProxyConfig.builder();
        Map<String, Object> properties = testProxyCore(proxyConfig);

        assertTrue(StringUtils.isNotBlank((CharSequence) properties.get("host")));
        assertTrue(StringUtils.isNotBlank((CharSequence) properties.get("protocol")));
        assertNotNull(properties.get("port"));
        assertFalse(properties.containsKey("user"));
        assertFalse(properties.containsKey("password"));
    }

    @Test
    public void testWithAuthProxy() {
        ProxyConfigBuilder proxyConfig = ProxyConfig.builder();
        proxyConfig.withProxyAuthentication(ProxyAuthentication.builder()
                .withUserName("user")
                .withPassword("pass")
                .build());
        Map<String, Object> properties = testProxyCore(proxyConfig);
        assertEquals("user", properties.get("user"));
        assertEquals("pass", properties.get("password"));
    }

    private Map<String, Object> testProxyCore(ProxyConfigBuilder proxyConfigBuilder) {
        proxyConfigBuilder.withServerHost("test");
        proxyConfigBuilder.withServerPort(3128);
        proxyConfigBuilder.withProtocol("http");
        cluster.setProxyConfigCrn("ANY_CRN");
        cluster.setEnvironmentCrn("ANY_CRN");
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(anyString(), anyString())).thenReturn(Optional.of(proxyConfigBuilder.build()));
        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, cluster);
        SaltPillarProperties pillarProperties = servicePillar.get(ProxyConfigProvider.PROXY_KEY);
        assertNotNull(pillarProperties);
        assertEquals(ProxyConfigProvider.PROXY_SLS_PATH, pillarProperties.getPath());
        return (Map<String, Object>) pillarProperties.getProperties().get(ProxyConfigProvider.PROXY_KEY);
    }
}
