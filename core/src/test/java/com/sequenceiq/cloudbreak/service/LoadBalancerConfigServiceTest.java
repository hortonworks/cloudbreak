package com.sequenceiq.cloudbreak.service;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class LoadBalancerConfigServiceTest {

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Mock
    private Blueprint blueprint;

    @InjectMocks
    private LoadBalancerConfigService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetKnoxGatewayWhenKnoxExplicitlyDefined() {
        Set<String> groups = Set.of("master");
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);

        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        Set<String> selectedGroups = underTest.getKnoxGatewayGroups(stack);
        assertEquals(groups, selectedGroups);
    }

    @Test
    public void testGetKnoxGatewayWhenKnoxImplicitlyDefinedByGatewayGroup() {
        Set<String> groups = Set.of("gateway");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("gateway");
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setInstanceGroups(Set.of(instanceGroup));

        when(blueprint.getBlueprintText()).thenReturn("{}");

        Set<String> selectedGroups = underTest.getKnoxGatewayGroups(stack);
        assertEquals(groups, selectedGroups);
    }

    @Test
    public void testGetKnoxGatewayWhenNoGateway() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("gateway");
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setInstanceGroups(Set.of(instanceGroup));

        when(blueprint.getBlueprintText()).thenReturn("{}");

        Set<String> selectedGroups = underTest.getKnoxGatewayGroups(stack);
        assert selectedGroups.isEmpty();
    }

    @Test
    public void testSelectPrivateLoadBalancer() {
        // Loop here to ensure we're really choosing the private load balancer every time, and not just
        // coincidentally choosing it because it shows up first in the set.
        for (int i = 1; i <= 10; i++) {
            Optional<LoadBalancer> loadBalancer = underTest.selectLoadBalancer(createLoadBalancers(), LoadBalancerType.PRIVATE);
            assert loadBalancer.isPresent();
            assert LoadBalancerType.PRIVATE.equals(loadBalancer.get().getType());
        }
    }

    @Test
    public void testSelectPublicLoadBalancer() {
        // Loop here to ensure we're really choosing the private load balancer every time, and not just
        // coincidentally choosing it because it shows up first in the set.
        for (int i = 1; i <= 10; i++) {
            Optional<LoadBalancer> loadBalancer = underTest.selectLoadBalancer(createLoadBalancers(), LoadBalancerType.PUBLIC);
            assert loadBalancer.isPresent();
            assert LoadBalancerType.PUBLIC.equals(loadBalancer.get().getType());
        }
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

    private Set<LoadBalancer> createLoadBalancers() {
        LoadBalancer privateLoadBalancer = new LoadBalancer();
        privateLoadBalancer.setType(LoadBalancerType.PRIVATE);
        LoadBalancer publicLoadBalancer = new LoadBalancer();
        publicLoadBalancer.setType(LoadBalancerType.PUBLIC);
        return Set.of(privateLoadBalancer, publicLoadBalancer);
    }
}
