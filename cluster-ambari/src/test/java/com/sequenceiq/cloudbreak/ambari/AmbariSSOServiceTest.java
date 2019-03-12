package com.sequenceiq.cloudbreak.ambari;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@RunWith(MockitoJUnitRunner.class)
public class AmbariSSOServiceTest {

    @InjectMocks
    private AmbariSSOService ambariSSOService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> captor;

    @Mock
    private AmbariClient ambariClient;

    @Before
    public void initTest() {
        Field knoxPort = ReflectionUtils.findField(AmbariSSOService.class, "knoxPort", String.class);
        knoxPort.setAccessible(true);
        ReflectionUtils.setField(knoxPort, ambariSSOService, "8443");
    }

    @Test
    public void setupSSOTest() throws IOException, URISyntaxException {
        Gateway gateway = new Gateway();
        gateway.setSsoProvider("/ssoprovider");
        gateway.setSsoType(SSOType.SSO_PROVIDER);
        gateway.setSignCert("cert");

        Cluster cluster = new Cluster();
        cluster.setName("cluster0");
        cluster.setGateway(gateway);

        ambariSSOService.setupSSO(ambariClient, cluster, "hostname");
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
    public void dontSetupSSOTest() throws IOException, URISyntaxException {
        Gateway gateway = new Gateway();

        Cluster cluster = new Cluster();
        cluster.setName("cluster0");
        cluster.setGateway(gateway);

        ambariSSOService.setupSSO(ambariClient, cluster, "1.1.1.1");
        verify(ambariClient, times(0)).configureSSO(captor.capture());
    }

}