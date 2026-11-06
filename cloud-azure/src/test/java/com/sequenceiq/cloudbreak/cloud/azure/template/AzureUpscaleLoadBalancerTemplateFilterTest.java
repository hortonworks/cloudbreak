package com.sequenceiq.cloudbreak.cloud.azure.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.network.fluent.models.LoadBalancerInner;
import com.azure.resourcemanager.network.fluent.models.ProbeInner;
import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerBackend;
import com.azure.resourcemanager.network.models.LoadBalancingRule;
import com.azure.resourcemanager.network.models.ProvisioningState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

@ExtendWith(MockitoExtension.class)
class AzureUpscaleLoadBalancerTemplateFilterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String RESOURCE_GROUP = "rg";

    private static final String ACCOUNT_ID = "accountId";

    private static final String PRIVATE_LB = "LoadBalancerstackPRIVATE";

    private static final String OUTBOUND_LB = "LoadBalancerstackOUTBOUND";

    private static final String PRIVATE_LB_DEPENDENCY = "[resourceId('Microsoft.Network/loadBalancers', '" + PRIVATE_LB + "')]";

    private static final String PRIVATE_POOL_REFERENCE = "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', '" + PRIVATE_LB
            + "', 'gateway-pool')]";

    private static final String TEMPLATE = """
            {
                "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
                "resources": [
                {"type": "Microsoft.Network/publicIPAddresses", "name": "%2$s-publicIp"},
                {"type": "Microsoft.Network/publicIPAddresses", "name": "gateway1-publicIp"},
                {"type": "Microsoft.Network/loadBalancers", "name": "%1$s",
                    "properties": {"backendAddressPools": [{"name": "gateway-pool"}],
                                "loadBalancingRules": [{"name": "port-443TCP-rule"}], "probes": [{"name": "port-8443-probe"}],
                                "frontendIPConfigurations": [{"name": "%1$s-frontend"}]}},
                {"type": "Microsoft.Network/loadBalancers", "name": "%2$s",
                    "dependsOn": ["[resourceId('Microsoft.Network/publicIPAddresses', '%2$s-publicIp')]"],
                    "properties": {"backendAddressPools": [{"name": "gateway-pool"}]}},
                {"type": "Microsoft.Network/networkInterfaces", "name": "gateway1-nic",
                    "dependsOn": ["[resourceId('Microsoft.Network/networkSecurityGroups', 'gateway-nsg')]",
                                "[resourceId('Microsoft.Network/publicIPAddresses', 'gateway1-publicIp')]",
                                "%3$s",
                                "[resourceId('Microsoft.Network/loadBalancers','%2$s')]"],
                    "properties": {"ipConfigurations": [{"properties": {"loadBalancerBackendAddressPools": [
                        {"id": "%4$s"},
                        {"id": "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', '%2$s', 'gateway-pool')]"}]}}]}}
                ]
            }""".formatted(PRIVATE_LB, OUTBOUND_LB, PRIVATE_LB_DEPENDENCY, PRIVATE_POOL_REFERENCE);

    @Mock
    private AzureClient azureClient;

    @Mock
    private EntitlementService entitlementService;

    private final AzureUpscaleLoadBalancerTemplateFilter underTest = new AzureUpscaleLoadBalancerTemplateFilter();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "skipExistingLoadBalancer", true);
        ReflectionTestUtils.setField(underTest, "entitlementService", entitlementService);
        lenient().when(entitlementService.isAzureUpscaleSkipExistingLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(true);
    }

    @Test
    void decideSkipsLoadBalancersThatExistWithAllPools() {
        mockLiveLoadBalancer(PRIVATE_LB, ProvisioningState.SUCCEEDED, "gateway-pool");
        mockLiveLoadBalancer(OUTBOUND_LB, ProvisioningState.SUCCEEDED, "gateway-pool");

        assertEquals(Set.of(PRIVATE_LB, OUTBOUND_LB), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
    }

    @Test
    void decideSkipsLoadBalancerWithEntriesNotManagedByCloudbreak() {
        LoadBalancer liveLoadBalancer = mockLiveLoadBalancer(PRIVATE_LB, ProvisioningState.SUCCEEDED, "gateway-pool", "proxy-pool");
        LoadBalancingRule foreignRule = mock(LoadBalancingRule.class);
        when(liveLoadBalancer.loadBalancingRules()).thenReturn(Map.of("port-443TCP-rule", foreignRule, "aaa-foreign-443", foreignRule));
        when(liveLoadBalancer.innerModel().probes()).thenReturn(List.of(new ProbeInner().withName("port-8443-probe"),
                new ProbeInner().withName("port-21000-probe")));
        mockLiveLoadBalancer(OUTBOUND_LB, ProvisioningState.SUCCEEDED, "gateway-pool");

        assertEquals(Set.of(PRIVATE_LB, OUTBOUND_LB), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
    }

    @Test
    void decideSkipsLoadBalancerEvenWhenCloudbreakEntriesAreMissingInAzure() {
        LoadBalancer liveLoadBalancer = mockLiveLoadBalancer(PRIVATE_LB, ProvisioningState.SUCCEEDED, "gateway-pool");
        LoadBalancingRule foreignRule = mock(LoadBalancingRule.class);
        when(liveLoadBalancer.loadBalancingRules()).thenReturn(Map.of("aaa-foreign-443", foreignRule));
        when(liveLoadBalancer.innerModel().probes()).thenReturn(List.of(new ProbeInner().withName("port-21000-probe")));
        mockLiveLoadBalancer(OUTBOUND_LB, ProvisioningState.SUCCEEDED, "gateway-pool");

        assertEquals(Set.of(PRIVATE_LB, OUTBOUND_LB), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
    }

    @Test
    void decideSkipsLoadBalancerWhenComparingItWithTheTemplateFails() {
        LoadBalancer liveLoadBalancer = mockLiveLoadBalancer(PRIVATE_LB, ProvisioningState.SUCCEEDED, "gateway-pool");
        when(liveLoadBalancer.loadBalancingRules()).thenThrow(new IllegalStateException("unexpected"));
        mockLiveLoadBalancer(OUTBOUND_LB, ProvisioningState.SUCCEEDED, "gateway-pool");

        assertEquals(Set.of(PRIVATE_LB, OUTBOUND_LB), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
    }

    @Test
    void decideKeepsLoadBalancerMissingInAzure() {
        when(azureClient.getLoadBalancer(RESOURCE_GROUP, PRIVATE_LB)).thenReturn(Optional.empty());
        mockLiveLoadBalancer(OUTBOUND_LB, ProvisioningState.SUCCEEDED, "gateway-pool");

        assertEquals(Set.of(OUTBOUND_LB), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
    }

    @Test
    void decideKeepsLoadBalancerWithMissingPool() {
        mockLiveLoadBalancer(PRIVATE_LB, ProvisioningState.SUCCEEDED, "proxy-pool");
        mockLiveLoadBalancer(OUTBOUND_LB, ProvisioningState.SUCCEEDED, "gateway-pool");

        assertEquals(Set.of(OUTBOUND_LB), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
    }

    @Test
    void decideKeepsLoadBalancerWithFailedProvisioningState() {
        mockLiveLoadBalancer(PRIVATE_LB, ProvisioningState.FAILED, "gateway-pool");
        mockLiveLoadBalancer(OUTBOUND_LB, ProvisioningState.SUCCEEDED, "gateway-pool");

        assertEquals(Set.of(OUTBOUND_LB), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
    }

    @Test
    void decideSkipsLegacyAddressPoolLoadBalancerOnlyWhenAddressPoolExists() {
        String legacyTemplate = """
                {"resources": [{"type": "Microsoft.Network/loadBalancers", "name": "%s",
                                "properties": {"backendAddressPools": [{"name": "address-pool"}]}}]}""".formatted(PRIVATE_LB);
        mockLiveLoadBalancer(PRIVATE_LB, ProvisioningState.SUCCEEDED, "address-pool");
        assertEquals(Set.of(PRIVATE_LB), underTest.decide(legacyTemplate, azureClient, RESOURCE_GROUP, ACCOUNT_ID));

        mockLiveLoadBalancer(PRIVATE_LB, ProvisioningState.SUCCEEDED, "gateway-pool");
        assertEquals(Set.of(), underTest.decide(legacyTemplate, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
    }

    @Test
    void decideReturnsEmptySetWhenGettingLoadBalancerFails() {
        when(azureClient.getLoadBalancer(RESOURCE_GROUP, PRIVATE_LB)).thenThrow(new ManagementException("forbidden", null));

        assertEquals(Set.of(), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
    }

    @Test
    void decideReturnsEmptySetForInvalidTemplate() {
        assertEquals(Set.of(), underTest.decide("{\"resources\": [,]}", azureClient, RESOURCE_GROUP, ACCOUNT_ID));
        verifyNoInteractions(azureClient);
    }

    @Test
    void decideReturnsEmptySetWhenDisabled() {
        ReflectionTestUtils.setField(underTest, "skipExistingLoadBalancer", false);

        assertEquals(Set.of(), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
        verifyNoInteractions(azureClient, entitlementService);
    }

    @Test
    void decideReturnsEmptySetWithoutEntitlement() {
        when(entitlementService.isAzureUpscaleSkipExistingLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(false);

        assertEquals(Set.of(), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
        verifyNoInteractions(azureClient);
    }

    @Test
    void decideReturnsEmptySetWhenEntitlementCheckFails() {
        when(entitlementService.isAzureUpscaleSkipExistingLoadBalancerEnabled(ACCOUNT_ID)).thenThrow(new IllegalStateException("UMS unavailable"));

        assertEquals(Set.of(), underTest.decide(TEMPLATE, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
        verifyNoInteractions(azureClient);
    }

    @Test
    void decideReturnsEmptySetForTemplateWithoutLoadBalancer() {
        assertEquals(Set.of(), underTest.decide("{\"resources\": []}", azureClient, RESOURCE_GROUP, ACCOUNT_ID));
        verifyNoInteractions(azureClient);
    }

    @Test
    void applyReturnsSameTemplateWhenNothingIsSkipped() {
        assertSame(TEMPLATE, underTest.apply(TEMPLATE, Set.of()));
    }

    @Test
    void applyRemovesSkippedLoadBalancersTheirPublicIpsAndDependencies() throws Exception {
        JsonNode result = OBJECT_MAPPER.readTree(underTest.apply(TEMPLATE, Set.of(PRIVATE_LB, OUTBOUND_LB)));

        assertEquals(Map.of("gateway1-publicIp", "Microsoft.Network/publicIPAddresses", "gateway1-nic", "Microsoft.Network/networkInterfaces"),
                resourceTypesByName(result));
        JsonNode nic = resource(result, "gateway1-nic");
        assertEquals(List.of("[resourceId('Microsoft.Network/networkSecurityGroups', 'gateway-nsg')]",
                "[resourceId('Microsoft.Network/publicIPAddresses', 'gateway1-publicIp')]"), texts(nic.path("dependsOn")));
        assertEquals(poolReferences(resource(OBJECT_MAPPER.readTree(TEMPLATE), "gateway1-nic")), poolReferences(nic));
        assertEquals(OBJECT_MAPPER.readTree(TEMPLATE).path("$schema"), result.path("$schema"));
    }

    @Test
    void decideAndApplyAcceptRawControlCharacterInStringLikeTheAzureSdk() throws Exception {
        String template = """
                {"resources": [
                    {"type": "Microsoft.Network/loadBalancers", "name": "%1$s", "tags": {"owner": "first\tsecond"},
                        "properties": {"backendAddressPools": [{"name": "gateway-pool"}]}},
                    {"type": "Microsoft.Network/networkInterfaces", "name": "gateway1-nic", "dependsOn": ["%2$s"]}
                ]}""".formatted(PRIVATE_LB, PRIVATE_LB_DEPENDENCY);
        mockLiveLoadBalancer(PRIVATE_LB, ProvisioningState.SUCCEEDED, "gateway-pool");

        assertEquals(Set.of(PRIVATE_LB), underTest.decide(template, azureClient, RESOURCE_GROUP, ACCOUNT_ID));
        JsonNode result = OBJECT_MAPPER.readTree(underTest.apply(template, Set.of(PRIVATE_LB)));

        assertEquals(Map.of("gateway1-nic", "Microsoft.Network/networkInterfaces"), resourceTypesByName(result));
    }

    @Test
    void applyKeepsLastValueOfDuplicateKeyLikeTheAzureSdk() throws Exception {
        String outboundPoolReference = "[resourceId('Microsoft.Network/loadBalancers/backendAddressPools', '" + OUTBOUND_LB + "', 'address-pool')]";
        String template = """
                {"resources": [
                    {"type": "Microsoft.Network/loadBalancers", "name": "%1$s"},
                    {"type": "Microsoft.Network/networkInterfaces", "name": "gateway1-nic", "dependsOn": ["%2$s"],
                        "properties": {"ipConfigurations": [{"properties": {"loadBalancerBackendAddressPools": [
                            {"id": "%3$s", "id": "%4$s"}]}}]}}
                ]}""".formatted(PRIVATE_LB, PRIVATE_LB_DEPENDENCY, PRIVATE_POOL_REFERENCE, outboundPoolReference);

        JsonNode nic = resource(OBJECT_MAPPER.readTree(underTest.apply(template, Set.of(PRIVATE_LB))), "gateway1-nic");

        assertEquals(List.of(outboundPoolReference), poolReferences(nic).findValuesAsText("id"));
    }

    @Test
    void applyKeepsLoadBalancerThatIsNotSkipped() throws Exception {
        JsonNode result = OBJECT_MAPPER.readTree(underTest.apply(TEMPLATE, Set.of(PRIVATE_LB)));

        assertEquals(Set.of(OUTBOUND_LB + "-publicIp", "gateway1-publicIp", OUTBOUND_LB, "gateway1-nic"), resourceTypesByName(result).keySet());
        List<String> nicDependencies = texts(resource(result, "gateway1-nic").path("dependsOn"));
        assertFalse(nicDependencies.contains(PRIVATE_LB_DEPENDENCY));
        assertTrue(nicDependencies.contains("[resourceId('Microsoft.Network/loadBalancers','" + OUTBOUND_LB + "')]"));
    }

    @Test
    void applyLeavesEmptyDependsOnWhenLoadBalancerWasTheOnlyDependency() throws Exception {
        String template = """
                {"resources": [
                    {"type": "Microsoft.Network/loadBalancers", "name": "%1$s"},
                    {"type": "Microsoft.Network/networkInterfaces", "name": "nic", "dependsOn": ["%2$s"]}
                ]}""".formatted(PRIVATE_LB, PRIVATE_LB_DEPENDENCY);

        JsonNode result = OBJECT_MAPPER.readTree(underTest.apply(template, Set.of(PRIVATE_LB)));

        assertEquals(1, result.path("resources").size());
        assertTrue(resource(result, "nic").path("dependsOn").isArray());
        assertTrue(resource(result, "nic").path("dependsOn").isEmpty());
    }

    @Test
    void applyAllowsEmptyResources() throws Exception {
        String template = """
                {"resources": [{"type": "Microsoft.Network/loadBalancers", "name": "%s"}]}""".formatted(PRIVATE_LB);

        JsonNode result = OBJECT_MAPPER.readTree(underTest.apply(template, Set.of(PRIVATE_LB)));

        assertTrue(result.path("resources").isArray());
        assertTrue(result.path("resources").isEmpty());
    }

    @Test
    void applyReturnsOriginalWhenAnotherDependencyRefersToTheLoadBalancer() {
        String template = """
                {"resources": [
                    {"type": "Microsoft.Network/loadBalancers", "name": "%1$s"},
                    {"type": "Microsoft.Network/networkInterfaces", "name": "nic",
                    "dependsOn": ["[concat(resourceId('Microsoft.Network/loadBalancers', '%1$s'), '/backendAddressPools/gateway-pool')]"]}
                ]}""".formatted(PRIVATE_LB);

        assertSame(template, underTest.apply(template, Set.of(PRIVATE_LB)));
    }

    @Test
    void applyReturnsOriginalWhenLoadBalancerPublicIpIsStillReferenced() {
        String template = """
                {"resources": [
                    {"type": "Microsoft.Network/publicIPAddresses", "name": "%1$s-publicIp"},
                    {"type": "Microsoft.Network/loadBalancers", "name": "%1$s"}
                ],
                    "outputs": {"ip": {"value": "[reference('%1$s-publicIp').ipAddress]"}}}""".formatted(OUTBOUND_LB);

        assertSame(template, underTest.apply(template, Set.of(OUTBOUND_LB)));
    }

    @Test
    void applyReturnsOriginalForInvalidTemplate() {
        String template = "{\"resources\": [{\"type\": \"Microsoft.Network/loadBalancers\",, }]}";

        assertSame(template, underTest.apply(template, Set.of(PRIVATE_LB)));
    }

    @Test
    void applyReturnsOriginalForTemplateWithoutResourcesArray() {
        String template = "{\"resources\": {}}";

        assertSame(template, underTest.apply(template, Set.of(PRIVATE_LB)));
    }

    private LoadBalancer mockLiveLoadBalancer(String name, ProvisioningState provisioningState, String... pools) {
        LoadBalancer liveLoadBalancer = mock(LoadBalancer.class);
        LoadBalancerInner inner = mock(LoadBalancerInner.class);
        when(inner.provisioningState()).thenReturn(provisioningState);
        when(liveLoadBalancer.innerModel()).thenReturn(inner);
        Map<String, LoadBalancerBackend> backends = Arrays.stream(pools)
                .collect(Collectors.toMap(Function.identity(), pool -> mock(LoadBalancerBackend.class)));
        lenient().when(liveLoadBalancer.backends()).thenReturn(backends);
        when(azureClient.getLoadBalancer(RESOURCE_GROUP, name)).thenReturn(Optional.of(liveLoadBalancer));
        return liveLoadBalancer;
    }

    private static Map<String, String> resourceTypesByName(JsonNode template) {
        return StreamSupport.stream(template.path("resources").spliterator(), false)
                .collect(Collectors.toMap(resource -> resource.path("name").asText(), resource -> resource.path("type").asText()));
    }

    private static JsonNode resource(JsonNode template, String name) {
        return StreamSupport.stream(template.path("resources").spliterator(), false)
                .filter(resource -> name.equals(resource.path("name").asText()))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> texts(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).toList();
    }

    private static JsonNode poolReferences(JsonNode nic) {
        return nic.path("properties").path("ipConfigurations").get(0).path("properties").path("loadBalancerBackendAddressPools");
    }
}
