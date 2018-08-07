package com.sequenceiq.cloudbreak.service.decorator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterProxyDecoratorTest {

    @InjectMocks
    private final ClusterProxyDecorator clusterProxyDecorator = new ClusterProxyDecorator();

    @Mock
    private ProxyConfigMapper mapper;

    @Mock
    private ProxyConfigService service;

    private final IdentityUser identityUser = new IdentityUser("test", "test", "test", null, "test", "test", new Date());

    private final Stack stack = new Stack();

    private Cluster cluster;

    @Before
    public void setUp() {
        when(service.getPublicProxyConfig(anyString(), eq(identityUser))).thenReturn(new ProxyConfig());
        cluster = new Cluster();
        stack.setPublicInAccount(true);
    }

    @Test
    public void testProxyNameProvided() {
        Cluster result = clusterProxyDecorator.prepareProxyConfig(cluster, identityUser, "test", stack);
        assertNotNull(result.getProxyConfig());
        Mockito.verify(service, Mockito.times(1)).getPublicProxyConfig(anyString(), eq(identityUser));
        Mockito.verify(service, Mockito.times(0)).create(any(IdentityUser.class), any(ProxyConfig.class));
    }

    @Test
    public void testNothingProvided() {
        Cluster result = clusterProxyDecorator.prepareProxyConfig(cluster, identityUser, null, stack);
        assertNull(result.getProxyConfig());
        Mockito.verify(service, Mockito.times(0)).create(any(IdentityUser.class), any(ProxyConfig.class));
        Mockito.verify(service, Mockito.times(0)).getPublicProxyConfig(anyString(), eq(identityUser));
    }
}