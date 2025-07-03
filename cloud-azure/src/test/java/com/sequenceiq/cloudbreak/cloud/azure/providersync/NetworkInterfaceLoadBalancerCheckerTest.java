package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.management.SubResource;
import com.azure.resourcemanager.network.fluent.models.BackendAddressPoolInner;
import com.azure.resourcemanager.network.fluent.models.NetworkInterfaceIpConfigurationInner;
import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerOutboundRule;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.NicIpConfiguration;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;

@ExtendWith(MockitoExtension.class)
class NetworkInterfaceLoadBalancerCheckerTest {

    private static final String RESOURCE_GROUP = "test-rg";

    private static final String NIC_PREFIX = "/subscriptions/sub1/resourceGroups/test-rg/providers/Microsoft.Network/networkInterfaces/";

    private static final String NIC_ID_1 = NIC_PREFIX + "nic1";

    private static final String NIC_ID_2 = NIC_PREFIX + "nic2";

    private static final String LB_PREFIX = "/subscriptions/sub1/resourceGroups/test-rg/providers/Microsoft.Network/loadBalancers/";

    private static final String LB_ID_1 = LB_PREFIX + "lb1";

    private static final String LB_ID_2 = LB_PREFIX + "lb2";

    private static final String BACKEND_POOL_ID_1 = LB_ID_1 + "/backendAddressPools/pool1";

    private static final String BACKEND_POOL_ID_2 = LB_ID_2 + "/backendAddressPools/pool2";

    @Mock
    private AzureClient azureClient;

    @InjectMocks
    private NetworkInterfaceLoadBalancerChecker underTest;

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldReturnErrorWhenNicIdsIsNull() {
        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(null, azureClient);

        assertEquals("No NIC IDs provided", result.getMessage());
        assertTrue(result.getNetworkInterfaceAnalyses().isEmpty());
        assertTrue(result.getCommonOutboundLoadBalancers().isEmpty());
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldReturnErrorWhenNicIdsIsEmpty() {
        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(Collections.emptyList(), azureClient);

        assertEquals("No NIC IDs provided", result.getMessage());
        assertTrue(result.getNetworkInterfaceAnalyses().isEmpty());
        assertTrue(result.getCommonOutboundLoadBalancers().isEmpty());
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldReturnErrorWhenNotAllNicsRetrieved() {
        List<String> nicIds = List.of(NIC_ID_1, NIC_ID_2);
        NetworkInterface nic1 = createMockNetworkInterface("nic1", NIC_ID_1);

        // Only return 1 NIC when 2 were requested
        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList())).thenReturn(List.of(nic1));

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().startsWith("Error during validation:"));
        assertTrue(result.getMessage().contains("Could not retrieve all specified network interfaces"));
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldReturnErrorWhenExceptionThrown() {
        List<String> nicIds = List.of(NIC_ID_1);

        when(azureClient.getNetworkInterfaceListByNames(anyString(), anyList()))
                .thenThrow(new RuntimeException("Azure API error"));

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().startsWith("Error during validation:"));
        assertTrue(result.getMessage().contains("Azure API error"));
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldReturnSuccessWhenCommonLoadBalancerFound() {
        List<String> nicIds = List.of(NIC_ID_1, NIC_ID_2);
        NetworkInterface nic1 = createMockNetworkInterfaceWithBackendPool("nic1", NIC_ID_1, BACKEND_POOL_ID_1);
        NetworkInterface nic2 = createMockNetworkInterfaceWithBackendPool("nic2", NIC_ID_2, BACKEND_POOL_ID_1);
        LoadBalancer lb1 = createMockLoadBalancerWithOutboundRule("lb1", LB_ID_1, BACKEND_POOL_ID_1);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1, nic2));
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Lists.newArrayList(lb1));
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().contains("Found 1 load balancer(s) providing outbound rules for all network interfaces"));
        assertTrue(result.getMessage().contains("lb1"));
        assertEquals(2, result.getNetworkInterfaceAnalyses().size());
        assertEquals(1, result.getCommonOutboundLoadBalancers().size());
        assertTrue(result.getCommonOutboundLoadBalancers().contains(lb1));
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldReturnFailureWhenNoCommonLoadBalancer() {
        List<String> nicIds = List.of(NIC_ID_1, NIC_ID_2);
        NetworkInterface nic1 = createMockNetworkInterfaceWithBackendPool("nic1", NIC_ID_1, BACKEND_POOL_ID_1);
        NetworkInterface nic2 = createMockNetworkInterfaceWithBackendPool("nic2", NIC_ID_2, BACKEND_POOL_ID_2);
        LoadBalancer lb1 = createMockLoadBalancerWithOutboundRule("lb1", LB_ID_1, BACKEND_POOL_ID_1);
        LoadBalancer lb2 = createMockLoadBalancerWithOutboundRule("lb2", LB_ID_2, BACKEND_POOL_ID_2);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1, nic2));
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Lists.newArrayList(lb1, lb2));
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertEquals("No common load balancer provides outbound rules for all network interfaces", result.getMessage());
        assertEquals(2, result.getNetworkInterfaceAnalyses().size());
        assertTrue(result.getCommonOutboundLoadBalancers().isEmpty());
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldReturnFailureWhenNicsWithoutOutbound() {
        List<String> nicIds = List.of(NIC_ID_1, NIC_ID_2);
        NetworkInterface nic1 = createMockNetworkInterfaceWithBackendPool("nic1", NIC_ID_1, BACKEND_POOL_ID_1);
        // No backend pools
        NetworkInterface nic2 = createMockNetworkInterface("nic2", NIC_ID_2);
        LoadBalancer lb1 = createMockLoadBalancerWithOutboundRule("lb1", LB_ID_1, BACKEND_POOL_ID_1);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1, nic2));
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Lists.newArrayList(lb1));
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().contains("Network interfaces without outbound load balancer rules"));
        assertTrue(result.getMessage().contains("nic2"));
        assertEquals(2, result.getNetworkInterfaceAnalyses().size());
        assertTrue(result.getCommonOutboundLoadBalancers().isEmpty());
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldHandleLoadBalancerWithoutOutboundRules() {
        List<String> nicIds = List.of(NIC_ID_1);
        NetworkInterface nic1 = createMockNetworkInterfaceWithBackendPool("nic1", NIC_ID_1, BACKEND_POOL_ID_1);
        LoadBalancer lb1 = createMockLoadBalancerWithoutOutboundRules("lb1", LB_ID_1);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1));
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Lists.newArrayList(lb1));
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().contains("Network interfaces without outbound load balancer rules"));
        assertTrue(result.getMessage().contains("nic1"));
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldHandleNicWithoutIpConfigurations() {
        List<String> nicIds = List.of(NIC_ID_1);
        NetworkInterface nic1 = createMockNetworkInterfaceWithoutIpConfigurations("nic1", NIC_ID_1);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1));
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Collections.emptyList());
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().contains("Network interfaces without outbound load balancer rules"));
        assertTrue(result.getMessage().contains("nic1"));
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldHandleNicWithNullBackendPools() {
        List<String> nicIds = List.of(NIC_ID_1);
        NetworkInterface nic1 = createMockNetworkInterfaceWithNullBackendPools("nic1", NIC_ID_1);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1));

        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Collections.emptyList());
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().contains("Network interfaces without outbound load balancer rules"));
        assertTrue(result.getMessage().contains("nic1"));
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldHandleMultipleCommonLoadBalancers() {
        List<String> nicIds = List.of(NIC_ID_1, NIC_ID_2);
        NetworkInterface nic1 = createMockNetworkInterfaceWithMultipleBackendPools("nic1", NIC_ID_1,
                List.of(BACKEND_POOL_ID_1, BACKEND_POOL_ID_2));
        NetworkInterface nic2 = createMockNetworkInterfaceWithMultipleBackendPools("nic2", NIC_ID_2,
                List.of(BACKEND_POOL_ID_1, BACKEND_POOL_ID_2));
        LoadBalancer lb1 = createMockLoadBalancerWithOutboundRule("lb1", LB_ID_1, BACKEND_POOL_ID_1);
        LoadBalancer lb2 = createMockLoadBalancerWithOutboundRule("lb2", LB_ID_2, BACKEND_POOL_ID_2);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1, nic2));
        List<LoadBalancer> loadBalancerList = Lists.newArrayList(lb1, lb2);
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(loadBalancerList);
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().contains("Found 2 load balancer(s) providing outbound rules for all network interfaces"));
        assertEquals(2, result.getCommonOutboundLoadBalancers().size());
    }

    @ParameterizedTest
    @MethodSource("invalidBackendPoolIdTestData")
    void extractLoadBalancerIdFromBackendPoolShouldHandleInvalidIds(String backendPoolId, String expectedResult) {
        // This tests the private method indirectly through the public interface
        List<String> nicIds = List.of(NIC_ID_1);
        NetworkInterface nic1 = createMockNetworkInterfaceWithBackendPool("nic1", NIC_ID_1, backendPoolId);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1));
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Collections.emptyList());
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        // If the backend pool ID is invalid, the NIC should have no load balancer associations
        NetworkInterfaceAnalysis analysis = result.getNetworkInterfaceAnalyses().get(NIC_ID_1);
        if (expectedResult == null) {
            assertTrue(analysis.getAllLoadBalancerIds().isEmpty());
        } else {
            assertFalse(analysis.getAllLoadBalancerIds().isEmpty());
        }
    }

    static Stream<Arguments> invalidBackendPoolIdTestData() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of("invalid-id", null),
                Arguments.of("/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/loadBalancers/lb1", null),
                Arguments.of("/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/loadBalancers/lb1/backendAddressPools/pool1",
                        "/subscriptions/sub1/resourceGroups/rg1/providers/Microsoft.Network/loadBalancers/lb1")
        );
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldHandleOutboundRuleWithNullBackendPool() {
        List<String> nicIds = List.of(NIC_ID_1);
        NetworkInterface nic1 = createMockNetworkInterfaceWithBackendPool("nic1", NIC_ID_1, BACKEND_POOL_ID_1);
        LoadBalancer lb1 = createMockLoadBalancerWithOutboundRuleWithNullBackendPool("lb1", LB_ID_1);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1));
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Lists.newArrayList(lb1));
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().contains("Network interfaces without outbound load balancer rules"));
        assertTrue(result.getMessage().contains("nic1"));
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldHandleSingleNicSuccessfully() {
        List<String> nicIds = List.of(NIC_ID_1);
        NetworkInterface nic1 = createMockNetworkInterfaceWithBackendPool("nic1", NIC_ID_1, BACKEND_POOL_ID_1);
        LoadBalancer lb1 = createMockLoadBalancerWithOutboundRule("lb1", LB_ID_1, BACKEND_POOL_ID_1);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1));
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Lists.newArrayList(lb1));
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().contains("Found 1 load balancer(s) providing outbound rules for all network interfaces"));
        assertEquals(1, result.getNetworkInterfaceAnalyses().size());
        assertEquals(1, result.getCommonOutboundLoadBalancers().size());
    }

    @Test
    void checkNetworkInterfacesWithCommonLoadBalancerShouldHandleEmptyLoadBalancerList() {
        List<String> nicIds = List.of(NIC_ID_1);
        NetworkInterface nic1 = createMockNetworkInterfaceWithBackendPool("nic1", NIC_ID_1, BACKEND_POOL_ID_1);

        when(azureClient.getNetworkInterfaceListByNames(eq(RESOURCE_GROUP), anyList()))
                .thenReturn(List.of(nic1));
        AzureListResult<LoadBalancer> mockLoadBalancers = createMockLoadBalancers(Collections.emptyList());
        when(azureClient.getLoadBalancers(RESOURCE_GROUP)).thenReturn(mockLoadBalancers);

        NetworkInterfaceCheckResult result = underTest.checkNetworkInterfacesWithCommonLoadBalancer(nicIds, azureClient);

        assertTrue(result.getMessage().contains("Network interfaces without outbound load balancer rules"));
        assertTrue(result.getMessage().contains("nic1"));
        assertEquals(1, result.getNetworkInterfaceAnalyses().size());
        assertTrue(result.getCommonOutboundLoadBalancers().isEmpty());
    }

    // Helper methods for creating mock objects

    private NetworkInterface createMockNetworkInterface(String name, String id) {
        NetworkInterface nic = mock(NetworkInterface.class);
        lenient().when(nic.name()).thenReturn(name);
        lenient().when(nic.id()).thenReturn(id);
        lenient().when(nic.resourceGroupName()).thenReturn(RESOURCE_GROUP);
        lenient().when(nic.ipConfigurations()).thenReturn(Collections.emptyMap());
        return nic;
    }

    private NetworkInterface createMockNetworkInterfaceWithoutIpConfigurations(String name, String id) {
        NetworkInterface nic = mock(NetworkInterface.class);
        when(nic.name()).thenReturn(name);
        when(nic.id()).thenReturn(id);
        lenient().when(nic.resourceGroupName()).thenReturn(RESOURCE_GROUP);
        when(nic.ipConfigurations()).thenReturn(Collections.emptyMap());
        return nic;
    }

    private NetworkInterface createMockNetworkInterfaceWithNullBackendPools(String name, String id) {
        NetworkInterface nic = mock(NetworkInterface.class);
        when(nic.name()).thenReturn(name);
        when(nic.id()).thenReturn(id);
        lenient().when(nic.resourceGroupName()).thenReturn(RESOURCE_GROUP);

        NicIpConfiguration ipConfig = mock(NicIpConfiguration.class);
        NetworkInterfaceIpConfigurationInner innerModel = mock(NetworkInterfaceIpConfigurationInner.class);
        when(ipConfig.innerModel()).thenReturn(innerModel);
        when(innerModel.loadBalancerBackendAddressPools()).thenReturn(null);

        Map<String, NicIpConfiguration> ipConfigs = Map.of("ipconfig1", ipConfig);
        when(nic.ipConfigurations()).thenReturn(ipConfigs);
        return nic;
    }

    private NetworkInterface createMockNetworkInterfaceWithBackendPool(String name, String id, String backendPoolId) {
        NetworkInterface nic = mock(NetworkInterface.class);
        when(nic.id()).thenReturn(id);
        lenient().when(nic.name()).thenReturn(name);

        BackendAddressPoolInner backendPool = mock(BackendAddressPoolInner.class);
        when(backendPool.id()).thenReturn(backendPoolId);

        NicIpConfiguration ipConfig = mock(NicIpConfiguration.class);
        NetworkInterfaceIpConfigurationInner innerModel = mock(NetworkInterfaceIpConfigurationInner.class);
        when(ipConfig.innerModel()).thenReturn(innerModel);
        when(innerModel.loadBalancerBackendAddressPools()).thenReturn(List.of(backendPool));

        Map<String, NicIpConfiguration> ipConfigs = Map.of("ipconfig1", ipConfig);
        when(nic.ipConfigurations()).thenReturn(ipConfigs);
        return nic;
    }

    private NetworkInterface createMockNetworkInterfaceWithMultipleBackendPools(String name, String id, List<String> backendPoolIds) {
        NetworkInterface nic = mock(NetworkInterface.class);
        lenient().when(nic.name()).thenReturn(name);
        lenient().when(nic.id()).thenReturn(id);
        lenient().when(nic.resourceGroupName()).thenReturn(RESOURCE_GROUP);

        List<BackendAddressPoolInner> backendPools = backendPoolIds.stream()
                .map(poolId -> {
                    BackendAddressPoolInner pool = mock(BackendAddressPoolInner.class);
                    when(pool.id()).thenReturn(poolId);
                    return pool;
                })
                .toList();

        NicIpConfiguration ipConfig = mock(NicIpConfiguration.class);
        NetworkInterfaceIpConfigurationInner innerModel = mock(NetworkInterfaceIpConfigurationInner.class);
        when(ipConfig.innerModel()).thenReturn(innerModel);
        when(innerModel.loadBalancerBackendAddressPools()).thenReturn(backendPools);

        Map<String, NicIpConfiguration> ipConfigs = Map.of("ipconfig1", ipConfig);
        when(nic.ipConfigurations()).thenReturn(ipConfigs);
        return nic;
    }

    private LoadBalancer createMockLoadBalancerWithOutboundRule(String name, String id, String backendPoolId) {
        LoadBalancer lb = mock(LoadBalancer.class);
        lenient().when(lb.name()).thenReturn(name);
        lenient().when(lb.id()).thenReturn(id);
        lenient().when(lb.resourceGroupName()).thenReturn(RESOURCE_GROUP);

        LoadBalancerOutboundRule outboundRule = mock(LoadBalancerOutboundRule.class);
        com.azure.resourcemanager.network.fluent.models.OutboundRuleInner outboundRuleInner =
                mock(com.azure.resourcemanager.network.fluent.models.OutboundRuleInner.class);
        SubResource backendPool = mock(SubResource.class);

        when(outboundRule.innerModel()).thenReturn(outboundRuleInner);
        when(outboundRuleInner.backendAddressPool()).thenReturn(backendPool);
        when(backendPool.id()).thenReturn(backendPoolId);

        Map<String, LoadBalancerOutboundRule> outboundRules = Map.of("rule1", outboundRule);
        when(lb.outboundRules()).thenReturn(outboundRules);

        return lb;
    }

    private LoadBalancer createMockLoadBalancerWithoutOutboundRules(String name, String id) {
        LoadBalancer lb = mock(LoadBalancer.class);
        lenient().when(lb.name()).thenReturn(name);
        when(lb.id()).thenReturn(id);
        lenient().when(lb.resourceGroupName()).thenReturn(RESOURCE_GROUP);
        when(lb.outboundRules()).thenReturn(Collections.emptyMap());
        return lb;
    }

    private LoadBalancer createMockLoadBalancerWithOutboundRuleWithNullBackendPool(String name, String id) {
        LoadBalancer lb = mock(LoadBalancer.class);
        lenient().when(lb.name()).thenReturn(name);
        when(lb.id()).thenReturn(id);
        lenient().when(lb.resourceGroupName()).thenReturn(RESOURCE_GROUP);

        LoadBalancerOutboundRule outboundRule = mock(LoadBalancerOutboundRule.class);
        com.azure.resourcemanager.network.fluent.models.OutboundRuleInner outboundRuleInner =
                mock(com.azure.resourcemanager.network.fluent.models.OutboundRuleInner.class);

        when(outboundRule.innerModel()).thenReturn(outboundRuleInner);
        when(outboundRuleInner.backendAddressPool()).thenReturn(null);

        Map<String, LoadBalancerOutboundRule> outboundRules = Map.of("rule1", outboundRule);
        when(lb.outboundRules()).thenReturn(outboundRules);

        return lb;
    }

    private AzureListResult<LoadBalancer> createMockLoadBalancers(List<LoadBalancer> loadBalancers) {
        AzureListResult<LoadBalancer> lbs = mock(AzureListResult.class);
        when(lbs.getAll()).thenReturn(loadBalancers);
        return lbs;
    }
}