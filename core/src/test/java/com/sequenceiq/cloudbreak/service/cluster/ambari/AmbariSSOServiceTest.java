package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariSSOServiceTest {

    @InjectMocks
    private AmbariSSOService ambariSSOService;

    @Mock
    private AmbariClientFactory ambariClientFactory;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private SecretService secretService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> captor;

    private AmbariClient ambariClient;

    @Before
    public void initTest() {
        ambariClient = mock(AmbariClient.class);
        when(ambariClientFactory.getAmbariClient(any(), any())).thenReturn(ambariClient);
        ambariClient = mock(AmbariClient.class);
        when(ambariClientFactory.getAmbariClient(any(), any())).thenReturn(ambariClient);
    }

    @Test
    public void setupSSOTest() {
        Gateway gateway = new Gateway();
        gateway.setSsoProvider("/ssoprovider");
        gateway.setSsoType(SSOType.SSO_PROVIDER);
        gateway.setSignCert("cert");

        Cluster cluster = new Cluster();
        cluster.setName("cluster0");
        cluster.setGateway(gateway);

        Stack stack = new Stack();
        stack.setName("stack0");
        stack.setId(2L);
        stack.setCluster(cluster);

        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getPublicAddress()).thenReturn("hostname");
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);

        ambariSSOService.setupSSO(stack, cluster);
        verify(ambariClient, times(1)).configureSSO(captor.capture());
        Map<String, Object> parameters = captor.getValue();
        assertThat(parameters, hasEntry("ambari.sso.provider.url", "https://hostname:8443/ssoprovider"));
        assertThat(parameters, hasEntry("ambari.sso.provider.certificate", "cert"));
        assertThat(parameters, hasEntry("ambari.sso.authentication.enabled", true));
        assertThat(parameters, hasEntry("ambari.sso.manage_services", true));
        assertThat(parameters, hasEntry("ambari.sso.enabled_services", "*"));
        assertThat(parameters, hasEntry("ambari.sso.jwt.cookieName", "hadoop-jwt"));
    }

    @Test
    public void dontSetupSSOTest() {
        Gateway gateway = new Gateway();

        Cluster cluster = new Cluster();
        cluster.setName("cluster0");
        cluster.setGateway(gateway);

        Stack stack = new Stack();
        stack.setName("stack0");
        stack.setId(2L);
        stack.setCluster(cluster);

        ambariSSOService.setupSSO(stack, cluster);
        verify(ambariClient, times(0)).configureSSO(captor.capture());
    }

}