package com.sequenceiq.cloudbreak.service.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.ProxyConfig.ProxyConfigBuilder;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
class ProxyConfigProviderTest {

    private static final String NO_PROXY_HOSTS = "noproxy.com";

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Workspace workspace;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private StackDto stackDto;

    @InjectMocks
    private ProxyConfigProvider proxyConfigProvider;

    private Cluster cluster;

    private Map<String, SaltPillarProperties> servicePillar;

    @BeforeEach
    void init() {
        cluster = new Cluster();
        servicePillar = new HashMap<>();
        cluster.setWorkspace(workspace);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(workspace.getTenant().getName()).thenReturn("tenantId");
        lenient().when(stackDto.getCreator().getUserCrn()).thenReturn("aUserCrn");
        lenient().when(stackDto.getTunnel()).thenReturn(Tunnel.CCMV2);
    }

    @Test
    void testNoProxyConfig() {
        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, stackDto);
        assertTrue(servicePillar.isEmpty());
    }

    @Test
    void testWithoutAuthProxy() {
        ProxyConfigBuilder proxyConfig = ProxyConfig.builder();
        Map<String, Object> properties = testProxyCore(proxyConfig);

        assertTrue(StringUtils.isNotBlank((CharSequence) properties.get("host")));
        assertTrue(StringUtils.isNotBlank((CharSequence) properties.get("protocol")));
        assertNotNull(properties.get("port"));
        assertFalse(properties.containsKey("user"));
        assertFalse(properties.containsKey("password"));
        assertEquals(NO_PROXY_HOSTS, properties.get("noProxyHosts"));
        assertEquals(Tunnel.CCMV2, properties.get("tunnel"));
    }

    @Test
    void testWithAuthProxy() {
        ProxyConfigBuilder proxyConfig = ProxyConfig.builder();
        proxyConfig.withProxyAuthentication(ProxyAuthentication.builder()
                .withUserName("user")
                .withPassword("pass")
                .build());
        Map<String, Object> properties = testProxyCore(proxyConfig);
        assertEquals("user", properties.get("user"));
        assertEquals("pass", properties.get("password"));
        assertEquals(NO_PROXY_HOSTS, properties.get("noProxyHosts"));
        assertEquals(Tunnel.CCMV2, properties.get("tunnel"));
    }

    private Map<String, Object> testProxyCore(ProxyConfigBuilder proxyConfigBuilder) {
        proxyConfigBuilder.withServerHost("test");
        proxyConfigBuilder.withServerPort(3128);
        proxyConfigBuilder.withProtocol("http");
        proxyConfigBuilder.withNoProxyHosts(NO_PROXY_HOSTS);
        cluster.setProxyConfigCrn("ANY_CRN");
        cluster.setEnvironmentCrn("ANY_CRN");
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(anyString(), anyString())).thenReturn(Optional.of(proxyConfigBuilder.build()));
        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, stackDto);
        SaltPillarProperties pillarProperties = servicePillar.get(ProxyConfigProvider.PROXY_KEY);
        assertNotNull(pillarProperties);
        assertEquals(ProxyConfigProvider.PROXY_SLS_PATH, pillarProperties.getPath());
        return (Map<String, Object>) pillarProperties.getProperties().get(ProxyConfigProvider.PROXY_KEY);
    }
}
