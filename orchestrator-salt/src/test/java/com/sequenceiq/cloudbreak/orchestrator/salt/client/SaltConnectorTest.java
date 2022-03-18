package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sequenceiq.cloudbreak.client.DisableProxyAuthFeature;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.client.SetProxyTimeoutFeature;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltErrorResolver;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.util.JaxRSUtil;

import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RestClientUtil.class, SaltConnector.class, JaxRSUtil.class})
public class SaltConnectorTest {

    private static final boolean DEBUG = false;

    private static final String SIGNATURE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEowIBAAKCAQEAnbam6//wlnpi3fHZQlRCUfjYpJVE9QEM9ld7nD5tXQwj9eFf\n" +
            "6oymz7yoQdwtUITxRqUaeu+PkyAwZ/YHG3CN7+W90+pGcPf2wl4WE9Pn/VfoB2KH\n" +
            "N5YlCLcHpLKu1fvP0azHwVtt1v1iuGMTxb/3s4HaA6yWpvnvoeE/fFmYdoiW7Xy7\n" +
            "GNbAuEYucaTJnU+Yac/l9IE1N69IviQepn9a1a8O2eoqBtZeKlqfCjsRqChwrU7j\n" +
            "CGC0HQuHoLgU1R5lnpoCJmMFiagoFpoPnSgvac1ZX0QFR7IjHl/1BEf3yG4Gq/jE\n" +
            "2tLuKfE6L/BE55DRLsaCRRHq8DVvTvZ80O2fdQIDAQABAoIBAHkADrcYCRTaVwoj\n" +
            "KGW8UZmki0pSf7JdmP1TBzJLrwppaxmVTUqdmMne21SUQWv6Y0apG1TggU4GrzzD\n" +
            "xJYn164LnIV+w4aeqAJdvyB9PwrfK9Smoklid41lJ4cT1BG2fa5HmoZdyDre8qO2\n" +
            "2A+rBbcCVCrnejonndOcBmI4N0IQurY+DrifAGAgWqX7ZBD4lDKDRcLOOZd32lIS\n" +
            "5kr8RQcwKLrzafqUo8owEwbMK2y4hzay7+eLHpFzz/j3nE6b3J3LsiIic0TteE+9\n" +
            "WTgo2hmUmk6tLWqSXma7gNnKzARBKc7//5wsbCfetDQFPr9x4jvwlhDdFxvaXmor\n" +
            "OL8epU0CgYEA1GcwVek7+thUM51ECGzaxcbBXnuE5N+nVkTbOtJ0JrkQ8kR/24Ge\n" +
            "rf2SBpRRCpjHQgZ/ZijGJt1QU8sPjC4SPm5CLF+UeqllefaqsGdp6uJmq/lQaDPk\n" +
            "XouiT1f54l0JNjdF9OZwjfYBtW3c7/g4t5Wt3JONbnNl4CORR4RD5esCgYEAvhXG\n" +
            "XVy0m04X+lwHDeolZsKbDh53Rh3pHWOyNWCriptltGCwKTC7pmKUcYHOgJ4Tfjvh\n" +
            "ZCB6YPNzPmZ9cveaP1gYVe16Xp/2chS5vU0fXTw2h1a167heYO8rmlVCZmCb/UOq\n" +
            "ETr8gsBkdQmNG9YsmIG/gyazOGKsnFFdGRoEWB8CgYBnKBqEjvrvcCMs0iNZiCyU\n" +
            "Q1xkm87GLY0iy9xBbDa3G8iMMRJ7tC8xx2YlReE6KUsU2P0Ey6a492Fs4METTsjT\n" +
            "g08mJ+F/1UeQfWkWtZLuXbiJq1MO2Kz/8gcJS+vzsUWpDB0wvL5LZAAeclYMQdyh\n" +
            "5NMEvrDAxNDkk8GaHzWuswKBgAMWxX905zZy4W7fGfo+6NavqOdk1VldWRFyMk4t\n" +
            "wPvipJd2zsYMlbARgAoMKvfqGbT+ch43bOWwbxn/fmBk56vJ2bNjxY5OGSonbhFG\n" +
            "dJEGTniKjzBCcb4vhMzgP9D8FuzZsaTQCZRyXe+M9S8Tnuvnh94wvc7Xw6hSpimn\n" +
            "Q5TPAoGBAJy3lfTHxu2PuYQL8BX3ZmFRZJX4fnw0aYIOjfIy0lOAtOZwdlVNoNLk\n" +
            "tgNgwg2xbrF4P3qrHVyNIi3+9x2jeNqs6xly2xg1icsu09oLCSuli6YGV2p2ONXD\n" +
            "tQ0Md1RivSo/sNjT1QsF7MJERCB/n8NprIddnIhoclFErmoMhmEA\n" +
            "-----END RSA PRIVATE KEY-----\n";

    private SaltConnector underTest;

    @Mock
    private SaltErrorResolver saltErrorResolver;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private Tracer tracer;

    @Mock
    private Client client;

    @Mock
    private WebTarget webTarget;

    @Mock
    private Invocation.Builder builder;

    @Mock
    private Response response;

    @Mock
    private GenericResponses genericResponses;

    @Captor
    private ArgumentCaptor<Entity<?>> entityArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        when(gatewayConfig.getHostname()).thenReturn("hostname");
        when(gatewayConfig.getServerCert()).thenReturn("servercert");
        when(gatewayConfig.getClientCert()).thenReturn("clientcert");
        when(gatewayConfig.getClientKey()).thenReturn("clientkey");
        when(gatewayConfig.getGatewayUrl()).thenReturn("gatewayurl");
        when(gatewayConfig.getSignatureKey()).thenReturn(SIGNATURE_KEY);
        PowerMockito.mockStatic(RestClientUtil.class);
        PowerMockito.doReturn(client).when(RestClientUtil.class, "createClient", anyString(), anyString(), anyString(), anyBoolean());
        when(webTarget.register(any(HttpAuthenticationFeature.class))).thenReturn(webTarget);
        when(webTarget.register(any(DisableProxyAuthFeature.class))).thenReturn(webTarget);
        when(webTarget.register(any(SetProxyTimeoutFeature.class))).thenReturn(webTarget);
        when(webTarget.register(any(ClientTracingFeature.class))).thenReturn(webTarget);
        when(client.target(anyString())).thenReturn(webTarget);

        when(webTarget.path(anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.post(any())).thenReturn(response);

        PowerMockito.mockStatic(JaxRSUtil.class);
        PowerMockito.doReturn(genericResponses).when(JaxRSUtil.class, "response", any(), eq(GenericResponses.class));

        underTest = new SaltConnector(gatewayConfig, saltErrorResolver, DEBUG, tracer);
    }

    @Test
    public void signatureHeaderShouldEqualToRequestBodySignature() {
        Pillar pillar = new Pillar("/cloudera-manager/settings.sls", Map.of("key", "value"), Set.of("target"));

        underTest.pillar(Set.of(""), pillar);

        verify(builder).header(eq("signature"), stringArgumentCaptor.capture());
        String signatureHeader = stringArgumentCaptor.getValue();

        verify(builder).post(entityArgumentCaptor.capture());
        Entity<String> value = (Entity<String>) entityArgumentCaptor.getValue();
        String requestBody = value.getEntity();

        Assert.assertEquals(underTest.toJson(pillar), requestBody);
        // TODO verify signature of request body teh same way as salt-bootstrap does
//        Assert.assertEquals(PkiUtil.generateSignature(SIGNATURE_KEY, requestBody.getBytes()), signatureHeader);
    }

}
