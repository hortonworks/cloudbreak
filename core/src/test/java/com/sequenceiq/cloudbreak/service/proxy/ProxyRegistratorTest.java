package com.sequenceiq.cloudbreak.service.proxy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.ecwid.consul.transport.RawResponse;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class ProxyRegistratorTest {

    @InjectMocks
    private final ProxyRegistrator underTest = new ProxyRegistrator();

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ConsulRawClient consulRawClient;

    private ConsulClient consulClient;

    @Before
    public void setUp() throws Exception {
        consulClient = new ConsulClient(consulRawClient);

        ReflectionTestUtils.setField(underTest, "consulClient", consulClient);
    }

    @Test
    public void testIsKnoxEnabledIsTrue() {
        Gateway gateway = new Gateway();
        gateway.setGatewayType(GatewayType.CENTRAL);

        boolean actual = underTest.isKnoxEnabled(gateway);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsKnoxEnabledIsFalseWhenGatewayTypeNotCentral() {
        Gateway gateway = new Gateway();
        gateway.setGatewayType(GatewayType.INDIVIDUAL);

        boolean actual = underTest.isKnoxEnabled(gateway);

        Assert.assertFalse(actual);
    }

    @Test
    public void testIsKnoxEnabledIsFalseWhenGatewayNull() {
        boolean actual = underTest.isKnoxEnabled(null);

        Assert.assertFalse(actual);
    }

    @Test
    public void testRegisterIfNeedWhenGatewayNull() {
        Cluster cluster = new Cluster();
        Stack stack = new Stack();
        stack.setCluster(cluster);

        underTest.registerIfNeed(stack);

        verify(stackUtil, Mockito.times(0)).extractAmbariIp(stack);
    }

    @Test
    public void testRegisterIfNeedWhenGatewayIndividual() {
        Gateway gateway = new Gateway();
        gateway.setGatewayType(GatewayType.INDIVIDUAL);

        Cluster cluster = new Cluster();
        cluster.setGateway(gateway);

        Stack stack = new Stack();
        stack.setCluster(cluster);

        underTest.registerIfNeed(stack);

        verify(stackUtil, Mockito.times(0)).extractAmbariIp(stack);
    }

    @Test
    public void testRegisterIfNeedWhenGatewayIsCentralNeed() {

        Gateway gateway = new Gateway();
        gateway.setGatewayType(GatewayType.CENTRAL);
        gateway.setPath("path");

        Cluster cluster = new Cluster();
        cluster.setGateway(gateway);

        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setName("StackName");

        when(stackUtil.extractAmbariIp(stack)).thenReturn("localhost");
        RawResponse rawResponse = new RawResponse(200, "OK", "true", 0L, true, 0L);
        when(consulRawClient.makePutRequest(
                eq("/v1/kv/traefik/backends/StackName/servers/gw/url"),
                eq("https://localhost:8443/path/"), any(), any(), any()))
                .thenReturn(rawResponse);
        when(consulRawClient.makePutRequest(
                eq("/v1/kv/traefik/frontends/StackName/backend"),
                eq("StackName"), any(), any(), any()))
                .thenReturn(rawResponse);
        when(consulRawClient.makePutRequest(
                eq("/v1/kv/traefik/frontends/StackName/passHostHeader"),
                eq("true"), any(), any(), any()))
                .thenReturn(rawResponse);
        when(consulRawClient.makePutRequest(
                eq("/v1/kv/traefik/frontends/StackName/routes/gw/rule"),
                eq("PathPrefix:/path/"), any(), any(), any()))
                .thenReturn(rawResponse);

        underTest.registerIfNeed(stack);

        verify(stackUtil, Mockito.times(1)).extractAmbariIp(stack);

    }
}
