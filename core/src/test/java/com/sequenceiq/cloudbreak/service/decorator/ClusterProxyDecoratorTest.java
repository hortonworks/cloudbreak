package com.sequenceiq.cloudbreak.service.decorator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterProxyDecoratorTest {

    @InjectMocks
    private ClusterProxyDecorator clusterProxyDecorator = new ClusterProxyDecorator();

    @Mock
    private ProxyConfigMapper mapper;

    @Mock
    private ProxyConfigService service;

    private IdentityUser identityUser = new IdentityUser("test", "test", "test", null, "test", "test", new Date());

    private Stack stack = new Stack();

    private Cluster cluster;

    @Before
    public void setUp() {
        when(mapper.mapRequestToEntity(any(ProxyConfigRequest.class), any(IdentityUser.class), anyBoolean())).thenReturn(new ProxyConfig());
        when(service.create(any(ProxyConfig.class))).thenReturn(new ProxyConfig());
        when(service.get(any(Long.class))).thenReturn(new ProxyConfig());
        cluster = new Cluster();
        stack.setPublicInAccount(true);
    }

    @Test
    public void testProxyIdProvided() {
        Cluster result = clusterProxyDecorator.prepareProxyConfig(cluster, identityUser, 1L, null, stack);
        assertNotNull(result.getProxyConfig());
        Mockito.verify(service, Mockito.times(1)).get(any(Long.class));
        Mockito.verify(service, Mockito.times(0)).create(any(ProxyConfig.class));
    }

    @Test
    public void testProxyRequestProvided() {
        ProxyConfigRequest request = new ProxyConfigRequest();
        Cluster result = clusterProxyDecorator.prepareProxyConfig(cluster, identityUser, null, request, stack);
        assertNotNull(result.getProxyConfig());
        Mockito.verify(service, Mockito.times(1)).create(any(ProxyConfig.class));
        Mockito.verify(service, Mockito.times(0)).get(any(Long.class));
    }

    @Test
    public void testNothingProvided() {
        Cluster result = clusterProxyDecorator.prepareProxyConfig(cluster, identityUser, null, null, stack);
        assertNull(result.getProxyConfig());
        Mockito.verify(service, Mockito.times(0)).create(any(ProxyConfig.class));
        Mockito.verify(service, Mockito.times(0)).get(any(Long.class));
    }
}