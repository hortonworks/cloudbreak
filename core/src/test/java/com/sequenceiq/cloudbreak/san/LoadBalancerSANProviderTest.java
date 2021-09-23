package com.sequenceiq.cloudbreak.san;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_4_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_4_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerType;

@ExtendWith(MockitoExtension.class)
class LoadBalancerSANProviderTest {

    public static final long ID = 1L;

    @InjectMocks
    private LoadBalancerSANProvider loadBalancerSANProvider;

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClouderaManagerRepo clouderaManagerRepo;

    private Stack stack;

    @BeforeEach
    void setUp() {
        loadBalancerSANProvider = new LoadBalancerSANProvider();
        stack = new Stack();
        stack.setId(ID);
        Cluster cluster = new Cluster();
        cluster.setId(ID);
        stack.setCluster(cluster);

        ReflectionTestUtils.setField(loadBalancerSANProvider, "loadBalancerConfigService", loadBalancerConfigService);
        ReflectionTestUtils.setField(loadBalancerSANProvider, "loadBalancerPersistenceService", loadBalancerPersistenceService);
        ReflectionTestUtils.setField(loadBalancerSANProvider, "clusterComponentConfigProvider", clusterComponentConfigProvider);

        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_4_3.getVersion());
    }

    @Test
    void newerVersionHasNoLoadBalancer() {
        assertTrue(loadBalancerSANProvider.getLoadBalancerSAN(stack).isEmpty());
    }

    @Test
    void olderVersionHasNoLoadBalancer() {
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_4_2.getVersion());
        assertTrue(loadBalancerSANProvider.getLoadBalancerSAN(stack).isEmpty());
    }

    @Test
    void newerVersionHasLoadBalancerWithDNS() {
        loadBalancerSetUp("foobar.timbuk2.com");
        assertEquals("DNS:foobar.timbuk2.com", loadBalancerSANProvider.getLoadBalancerSAN(stack).get());
    }

    @Test
    void newerVersionHasLoadBalancerWithoutDNS() {
        loadBalancerSetUp(null);
        assertEquals("IP:10.10.10.10", loadBalancerSANProvider.getLoadBalancerSAN(stack).get());
    }

    private void loadBalancerSetUp(String dns) {
        LoadBalancer loadBalancer = new LoadBalancer();
        if (dns != null) {
            loadBalancer.setDns("foobar.timbuk2.com");
        }
        loadBalancer.setIp("10.10.10.10");
        Set<LoadBalancer> loadBalancers = new HashSet<>();
        loadBalancers.add(loadBalancer);
        when(loadBalancerPersistenceService.findByStackId(ID)).thenReturn(loadBalancers);
        when(loadBalancerConfigService.selectLoadBalancer(loadBalancers, LoadBalancerType.PUBLIC)).thenReturn(Optional.of(loadBalancer));
    }
}