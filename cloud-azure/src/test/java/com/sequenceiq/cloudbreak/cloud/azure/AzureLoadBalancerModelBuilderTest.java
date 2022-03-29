package com.sequenceiq.cloudbreak.cloud.azure;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

class AzureLoadBalancerModelBuilderTest {
    public static final String STACK_NAME = "stack-name";

    public static final String INSTANCE_GROUP_NAME = "bderriso-linz-sdx-gateway";

    public static final String INSTANCE_NAME = "bderriso-linz-sdx-g0";

    public static final String LOAD_BALANCERS_KEY = "loadBalancers";

    public static final String LOAD_BALANCER_STACK_NAME = "LoadBalancerstack-namePRIVATE";

    public static final String LOAD_BALANCER_MAPPING_KEY = "loadBalancerMapping";

    private AzureLoadBalancerModelBuilder underTest;

    @Test
    void testGetModel() {
        CloudStack mockCloudStack = mock(CloudStack.class);
        Group targetGroup = new Group(INSTANCE_GROUP_NAME,
                InstanceGroupType.GATEWAY,
                List.of(new CloudInstance(INSTANCE_NAME, null, null, "subnet-1", "az1")),
                null,
                null,
                null,
                null,
                null,
                64,
                null,
                createGroupNetwork(),
                emptyMap());
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
        cloudLoadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 443), Set.of(targetGroup));
        when(mockCloudStack.getLoadBalancers()).thenReturn(List.of(cloudLoadBalancer));

        underTest = new AzureLoadBalancerModelBuilder(mockCloudStack, STACK_NAME);

        Map<String, Object> result = underTest.buildModel();

        assertTrue(result.containsKey(LOAD_BALANCERS_KEY));
        assertTrue(result.get(LOAD_BALANCERS_KEY) instanceof List);
        List<AzureLoadBalancer> loadBalancers = (List<AzureLoadBalancer>) result.get(LOAD_BALANCERS_KEY);
        assertEquals(1, loadBalancers.size());
        AzureLoadBalancer lb = loadBalancers.get(0);
        assertEquals(LOAD_BALANCER_STACK_NAME, lb.getName());
        assertEquals(LoadBalancerType.PRIVATE, lb.getType());

        assertTrue(result.containsKey(LOAD_BALANCER_MAPPING_KEY));
        assertTrue(result.get(LOAD_BALANCER_MAPPING_KEY) instanceof Map);
        Map<String, List<AzureLoadBalancer>> mapping = (Map<String, List<AzureLoadBalancer>>) result.get(LOAD_BALANCER_MAPPING_KEY);
        List<AzureLoadBalancer> mappingList = mapping.get(INSTANCE_GROUP_NAME);
        assertEquals(1, mappingList.size());
        assertEquals(LOAD_BALANCER_STACK_NAME, mappingList.get(0).getName());
    }

    @Test
    void testGetModelWithDefaultSku() {
        CloudStack mockCloudStack = mock(CloudStack.class);
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer();
        when(mockCloudStack.getLoadBalancers()).thenReturn(List.of(cloudLoadBalancer));

        underTest = new AzureLoadBalancerModelBuilder(mockCloudStack, STACK_NAME);

        Map<String, Object> result = underTest.buildModel();

        assertTrue(result.containsKey(LOAD_BALANCERS_KEY));
        assertTrue(result.get(LOAD_BALANCERS_KEY) instanceof List);
        List<AzureLoadBalancer> loadBalancers = (List<AzureLoadBalancer>) result.get(LOAD_BALANCERS_KEY);
        assertEquals(1, loadBalancers.size());
        AzureLoadBalancer lb = loadBalancers.get(0);
        assertEquals(LoadBalancerSku.getDefault(), lb.getSku());
    }

    @Test
    void testGetModelWithBasicSku() {
        CloudStack mockCloudStack = mock(CloudStack.class);
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(LoadBalancerSku.BASIC);
        when(mockCloudStack.getLoadBalancers()).thenReturn(List.of(cloudLoadBalancer));

        underTest = new AzureLoadBalancerModelBuilder(mockCloudStack, STACK_NAME);

        Map<String, Object> result = underTest.buildModel();

        assertTrue(result.containsKey(LOAD_BALANCERS_KEY));
        assertTrue(result.get(LOAD_BALANCERS_KEY) instanceof List);
        List<AzureLoadBalancer> loadBalancers = (List<AzureLoadBalancer>) result.get(LOAD_BALANCERS_KEY);
        assertEquals(1, loadBalancers.size());
        AzureLoadBalancer lb = loadBalancers.get(0);
        assertEquals(LoadBalancerSku.BASIC, lb.getSku());
    }

    @Test
    void testGetModelWithStandardSku() {
        CloudStack mockCloudStack = mock(CloudStack.class);
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(LoadBalancerSku.STANDARD);
        when(mockCloudStack.getLoadBalancers()).thenReturn(List.of(cloudLoadBalancer));

        underTest = new AzureLoadBalancerModelBuilder(mockCloudStack, STACK_NAME);

        Map<String, Object> result = underTest.buildModel();

        assertTrue(result.containsKey(LOAD_BALANCERS_KEY));
        assertTrue(result.get(LOAD_BALANCERS_KEY) instanceof List);
        List<AzureLoadBalancer> loadBalancers = (List<AzureLoadBalancer>) result.get(LOAD_BALANCERS_KEY);
        assertEquals(1, loadBalancers.size());
        AzureLoadBalancer lb = loadBalancers.get(0);
        assertEquals(LoadBalancerSku.STANDARD, lb.getSku());
    }

    @Test
    void testGetModelWithOutboundLoadBalancer() {
        CloudStack mockCloudStack = mock(CloudStack.class);
        CloudLoadBalancer privateLoadBalancer = createCloudLoadBalancer(LoadBalancerSku.STANDARD);
        CloudLoadBalancer outboundLoadBalancer = createOutboundCloudLoadBalancer();
        when(mockCloudStack.getLoadBalancers()).thenReturn(List.of(privateLoadBalancer, outboundLoadBalancer));

        underTest = new AzureLoadBalancerModelBuilder(mockCloudStack, STACK_NAME);

        Map<String, Object> result = underTest.buildModel();

        assertTrue(result.containsKey(LOAD_BALANCERS_KEY));
        assertTrue(result.get(LOAD_BALANCERS_KEY) instanceof List);
        List<AzureLoadBalancer> loadBalancers = (List<AzureLoadBalancer>) result.get(LOAD_BALANCERS_KEY);
        assertEquals(2, loadBalancers.size());
        assertTrue(loadBalancers.stream().allMatch(lb -> LoadBalancerSku.STANDARD.equals(lb.getSku())));
        assertTrue(loadBalancers.stream().anyMatch(lb -> LoadBalancerType.PRIVATE.equals(lb.getType())));
        assertTrue(loadBalancers.stream().anyMatch(lb -> LoadBalancerType.OUTBOUND.equals(lb.getType())));
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }

    private CloudLoadBalancer createCloudLoadBalancer() {
        return createCloudLoadBalancer(null);
    }

    private CloudLoadBalancer createOutboundCloudLoadBalancer() {
        return createCloudLoadBalancer(LoadBalancerSku.STANDARD, LoadBalancerType.OUTBOUND);
    }

    private CloudLoadBalancer createCloudLoadBalancer(LoadBalancerSku sku) {
        return createCloudLoadBalancer(sku, LoadBalancerType.PRIVATE);
    }

    private CloudLoadBalancer createCloudLoadBalancer(LoadBalancerSku sku, LoadBalancerType type) {
        Group targetGroup = new Group(INSTANCE_GROUP_NAME,
                InstanceGroupType.GATEWAY,
                List.of(new CloudInstance(INSTANCE_NAME, null, null, "subnet-1", "az1")),
                null,
                null,
                null,
                null,
                null,
                64,
                null,
                createGroupNetwork(),
                emptyMap());
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(type, sku);
        cloudLoadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(443, 443), Set.of(targetGroup));
        return cloudLoadBalancer;
    }
}