package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.azure.resourcemanager.network.fluent.models.LoadBalancerInner;
import com.azure.resourcemanager.network.fluent.models.ProbeInner;
import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerBackend;
import com.azure.resourcemanager.network.models.LoadBalancerFrontend;
import com.azure.resourcemanager.network.models.LoadBalancerInboundNatRule;
import com.azure.resourcemanager.network.models.LoadBalancerOutboundRule;
import com.azure.resourcemanager.network.models.LoadBalancingRule;

class AzureLoadBalancerChildCollectionTest {

    @Test
    void testArmPropertiesMatchTheTemplate() {
        assertEquals(List.of("backendAddressPools", "loadBalancingRules", "probes", "frontendIPConfigurations", "outboundRules", "inboundNatRules"),
                Arrays.stream(AzureLoadBalancerChildCollection.values()).map(AzureLoadBalancerChildCollection::getArmProperty).toList());
    }

    @Test
    void testLiveNamesAreReadFromTheMatchingCollection() {
        LoadBalancer loadBalancer = mock(LoadBalancer.class);
        LoadBalancerInner inner = mock(LoadBalancerInner.class);
        when(loadBalancer.innerModel()).thenReturn(inner);
        when(loadBalancer.backends()).thenReturn(Map.of("master-pool", mock(LoadBalancerBackend.class)));
        when(loadBalancer.loadBalancingRules()).thenReturn(Map.of("port-443TCP-rule", mock(LoadBalancingRule.class)));
        when(inner.probes()).thenReturn(List.of(new ProbeInner().withName("port-8443-probe"), new ProbeInner()));
        when(loadBalancer.frontends()).thenReturn(Map.of("lb-frontend", mock(LoadBalancerFrontend.class)));
        when(loadBalancer.outboundRules()).thenReturn(Map.of("outbound-rule", mock(LoadBalancerOutboundRule.class)));
        when(loadBalancer.inboundNatRules()).thenReturn(Map.of("nat-rule", mock(LoadBalancerInboundNatRule.class)));

        Map<AzureLoadBalancerChildCollection, Set<String>> liveNames = Arrays.stream(AzureLoadBalancerChildCollection.values())
                .collect(Collectors.toMap(collection -> collection, collection -> collection.getLiveNames(loadBalancer)));

        assertEquals(Map.of(
                AzureLoadBalancerChildCollection.BACKEND_ADDRESS_POOLS, Set.of("master-pool"),
                AzureLoadBalancerChildCollection.LOAD_BALANCING_RULES, Set.of("port-443TCP-rule"),
                AzureLoadBalancerChildCollection.PROBES, Set.of("port-8443-probe"),
                AzureLoadBalancerChildCollection.FRONTEND_IP_CONFIGURATIONS, Set.of("lb-frontend"),
                AzureLoadBalancerChildCollection.OUTBOUND_RULES, Set.of("outbound-rule"),
                AzureLoadBalancerChildCollection.INBOUND_NAT_RULES, Set.of("nat-rule")), liveNames);
    }

    @Test
    void testProbesWithoutInnerProbeListAreEmpty() {
        LoadBalancer loadBalancer = mock(LoadBalancer.class);
        when(loadBalancer.innerModel()).thenReturn(mock(LoadBalancerInner.class));

        assertEquals(Set.of(), AzureLoadBalancerChildCollection.PROBES.getLiveNames(loadBalancer));
    }
}
