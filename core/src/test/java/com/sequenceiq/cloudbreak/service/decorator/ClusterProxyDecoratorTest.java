package com.sequenceiq.cloudbreak.service.decorator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterProxyDecoratorTest {

    @InjectMocks
    private final ClusterProxyDecorator clusterProxyDecorator = new ClusterProxyDecorator();

    @Mock
    private ProxyConfigMapper mapper;

    @Mock
    private ProxyConfigService service;

    @Mock
    private User user;

    private final CloudbreakUser cloudbreakUser = new CloudbreakUser("test", "test", "test", "test");

    private Cluster cluster;

    @Before
    public void setUp() {
        when(service.getByNameForWorkspace(anyString(), any(Workspace.class))).thenReturn(new ProxyConfig());
        cluster = new Cluster();
        cluster.setWorkspace(new Workspace());
    }

    @Test
    public void testProxyNameProvided() {
        Cluster result = clusterProxyDecorator.prepareProxyConfig(cluster, "test");
        assertNotNull(result.getProxyConfig());
        Mockito.verify(service, Mockito.times(1)).getByNameForWorkspace(anyString(), any(Workspace.class));
        Mockito.verify(service, Mockito.times(0)).create(any(ProxyConfig.class), anyLong(), eq(user));
    }

    @Test
    public void testNothingProvided() {
        Cluster result = clusterProxyDecorator.prepareProxyConfig(cluster, null);
        assertNull(result.getProxyConfig());
        Mockito.verify(service, Mockito.times(0)).create(any(ProxyConfig.class), anyLong(), eq(user));
        Mockito.verify(service, Mockito.times(0)).getByNameForWorkspace(anyString(), any(Workspace.class));
    }
}