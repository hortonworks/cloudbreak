package com.sequenceiq.cloudbreak.service.proxy;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ConsulClient.class)
public class ProxyRegistratorTest {

    @InjectMocks
    private final ProxyRegistrator underTest = new ProxyRegistrator();

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ConsulClient consulClient;

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

        underTest.registerIfNeed(stack);

        verify(stackUtil, Mockito.times(1)).extractAmbariIp(stack);
        verify(consulClient, Mockito.times(1)).setKVValue("traefik/backends/StackName/servers/gw/url", "https://localhost:8443/path/");
        verify(consulClient, Mockito.times(1)).setKVValue("traefik/frontends/StackName/backend", "StackName");
        verify(consulClient, Mockito.times(1)).setKVValue("traefik/frontends/StackName/passHostHeader", "true");
        verify(consulClient, Mockito.times(1)).setKVValue("traefik/frontends/StackName/routes/gw/rule", "PathPrefix:/path/");
    }
}
