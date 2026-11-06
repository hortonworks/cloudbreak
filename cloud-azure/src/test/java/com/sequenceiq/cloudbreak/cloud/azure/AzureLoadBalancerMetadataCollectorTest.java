package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.network.fluent.models.BackendAddressPoolInner;
import com.azure.resourcemanager.network.models.LoadBalancerBackend;
import com.azure.resourcemanager.network.models.LoadBalancingRule;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancerResourceNames;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.instance.AvailabilitySetNameService;

@ExtendWith(MockitoExtension.class)
public class AzureLoadBalancerMetadataCollectorTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String RESOURCE_GROUP = "my-resource-group";

    private static final String LB_NAME = "load-balancer-name";

    private static final String GROUP_NAME = "group-name-%d";

    private static final String GROUP_PATTERN = GROUP_NAME + "%s";

    private static final String BACKEND_GROUP_PATTERN = GROUP_PATTERN + "%s";

    private static final String POOL_SUFFIX = "-pool";

    private static final String AS_SUFFIX = "-as";

    private static final String GATEWAY_SUFFIX = "-gateway";

    private static final String STACK_NAME = "stackName";

    @Mock
    private AzureClient azureClient;

    @Mock
    private AvailabilitySetNameService availabilitySetNameService;

    @InjectMocks
    private AzureLoadBalancerMetadataCollector underTest;

    @Test
    public void testCollectInternalLoadBalancer() {
        int numPorts = 1;
        AuthenticatedContext ac = authenticatedContext();
        Map<String, LoadBalancingRule> rules = getLoadBalancingRules(numPorts);
        String availabilitySetName = String.format(GROUP_PATTERN, 0, AS_SUFFIX);
        String groupName = String.format(GROUP_NAME, 0);

        Map<String, Object> expectedParameters = Map.of(
                AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME,
                AzureLoadBalancerMetadataView.AVAILABILITY_SET_PREFIX + 0, availabilitySetName
        );

        when(azureClient.getLoadBalancerRules(eq(RESOURCE_GROUP), eq(LB_NAME))).thenReturn(rules);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), anyString())).thenReturn(Optional.ofNullable(mock(AvailabilitySet.class)));
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName))).thenReturn(availabilitySetName);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        assertEquals(expectedParameters, parameters);
    }

    @Test
    public void testCollectLoadBalancerMultiplePorts() {
        int numPorts = 3;
        AuthenticatedContext ac = authenticatedContext();
        Map<String, LoadBalancingRule> rules = getLoadBalancingRules(numPorts);
        String availabilitySetName0 = String.format(GROUP_PATTERN, 0, AS_SUFFIX);
        String availabilitySetName1 = String.format(GROUP_PATTERN, 1, AS_SUFFIX);
        String availabilitySetName2 = String.format(GROUP_PATTERN, 2, AS_SUFFIX);
        String groupName0 = String.format(GROUP_NAME, 0);
        String groupName1 = String.format(GROUP_NAME, 1);
        String groupName2 = String.format(GROUP_NAME, 2);

        Map<String, Object> expectedParameters = Map.of(
                AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME,
                AzureLoadBalancerMetadataView.AVAILABILITY_SET_PREFIX + 0, availabilitySetName0,
                AzureLoadBalancerMetadataView.AVAILABILITY_SET_PREFIX + 1, availabilitySetName1,
                AzureLoadBalancerMetadataView.AVAILABILITY_SET_PREFIX + 2, availabilitySetName2
        );

        when(azureClient.getLoadBalancerRules(eq(RESOURCE_GROUP), eq(LB_NAME))).thenReturn(rules);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), anyString())).thenReturn(Optional.ofNullable(mock(AvailabilitySet.class)));
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName0))).thenReturn(availabilitySetName0);
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName1))).thenReturn(availabilitySetName1);
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName2))).thenReturn(availabilitySetName2);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        assertEquals(expectedParameters, parameters);
    }

    @Test
    public void testCollectLoadBalancerMissingAvailabilitySet() {
        int numPorts = 3;
        AuthenticatedContext ac = authenticatedContext();
        Map<String, LoadBalancingRule> rules = getLoadBalancingRules(numPorts);
        String availabilitySetName0 = String.format(GROUP_PATTERN, 0, AS_SUFFIX);
        String availabilitySetName1 = String.format(GROUP_PATTERN, 1, AS_SUFFIX);
        String availabilitySetName2 = String.format(GROUP_PATTERN, 2, AS_SUFFIX);
        String groupName0 = String.format(GROUP_NAME, 0);
        String groupName1 = String.format(GROUP_NAME, 1);
        String groupName2 = String.format(GROUP_NAME, 2);

        Map<String, Object> expectedParameters = new HashMap<>();
        expectedParameters.put(AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME);
        expectedParameters.put(AzureLoadBalancerMetadataView.AVAILABILITY_SET_PREFIX + 0, availabilitySetName0);
        expectedParameters.put(AzureLoadBalancerMetadataView.AVAILABILITY_SET_PREFIX + 1, null);
        expectedParameters.put(AzureLoadBalancerMetadataView.AVAILABILITY_SET_PREFIX + 2, availabilitySetName2);

        when(azureClient.getLoadBalancerRules(eq(RESOURCE_GROUP), eq(LB_NAME))).thenReturn(rules);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName0))).thenReturn(Optional.ofNullable(mock(AvailabilitySet.class)));
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName1))).thenReturn(Optional.empty());
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName2))).thenReturn(Optional.ofNullable(mock(AvailabilitySet.class)));
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName0))).thenReturn(availabilitySetName0);
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName1))).thenReturn(availabilitySetName1);
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName2))).thenReturn(availabilitySetName2);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        assertEquals(expectedParameters, parameters);
    }

    @Test
    public void testCollectLoadBalancerIgnoresRuleWithoutBackendPool() {
        AuthenticatedContext ac = authenticatedContext();
        LoadBalancingRule ruleWithoutBackend = mock(LoadBalancingRule.class);
        lenient().when(ruleWithoutBackend.name()).thenReturn("aaa-foreign-21050");
        lenient().when(ruleWithoutBackend.backendPort()).thenReturn(21050);
        Map<String, LoadBalancingRule> rules = new LinkedHashMap<>();
        rules.put("aaa-foreign-21050", ruleWithoutBackend);
        rules.put("port-443TCP-rule", loadBalancingRule("port-443TCP-rule", 443, "gateway-pool"));
        String availabilitySetName = "stackName-gateway-as";

        when(azureClient.getLoadBalancerRules(eq(RESOURCE_GROUP), eq(LB_NAME))).thenReturn(rules);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName))).thenReturn(Optional.of(mock(AvailabilitySet.class)));
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq("gateway"))).thenReturn(availabilitySetName);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        assertEquals(Map.of(
                AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME,
                AzureLoadBalancerMetadataView.getAvailabilitySetParam(443), availabilitySetName), parameters);
    }

    @ParameterizedTest
    @CsvSource({
            "port-443TCP-rule, true",
            "port-443TCP-rule, false",
            "port-443-rule, true",
            "port-443-rule, false"
    })
    public void testCollectLoadBalancerPrefersCloudbreakRuleOnBackendPortCollision(String cloudbreakRuleName, boolean foreignRuleFirst) {
        AuthenticatedContext ac = authenticatedContext();
        LoadBalancingRule cloudbreakRule = loadBalancingRule(cloudbreakRuleName, 443, "gateway-pool");
        LoadBalancingRule foreignRule = loadBalancingRule("aaa-foreign-443", 443, "proxy-pool");
        Map<String, LoadBalancingRule> rules = new LinkedHashMap<>();
        if (foreignRuleFirst) {
            rules.put(foreignRule.name(), foreignRule);
            rules.put(cloudbreakRule.name(), cloudbreakRule);
        } else {
            rules.put(cloudbreakRule.name(), cloudbreakRule);
            rules.put(foreignRule.name(), foreignRule);
        }
        String availabilitySetName = "stackName-gateway-as";

        when(azureClient.getLoadBalancerRules(eq(RESOURCE_GROUP), eq(LB_NAME))).thenReturn(rules);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName))).thenReturn(Optional.of(mock(AvailabilitySet.class)));
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq("gateway"))).thenReturn(availabilitySetName);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        assertEquals(Map.of(
                AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME,
                AzureLoadBalancerMetadataView.getAvailabilitySetParam(443), availabilitySetName), parameters);
    }

    @Test
    public void testCollectLoadBalancerRemovesOnlyThePoolSuffixFromTheGroupName() {
        AuthenticatedContext ac = authenticatedContext();
        Map<String, LoadBalancingRule> rules = Map.of("port-443TCP-rule", loadBalancingRule("port-443TCP-rule", 443, "my-pool-group-pool"));
        String availabilitySetName = "stackName-my-pool-group-as";

        when(azureClient.getLoadBalancerRules(eq(RESOURCE_GROUP), eq(LB_NAME))).thenReturn(rules);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName))).thenReturn(Optional.of(mock(AvailabilitySet.class)));
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq("my-pool-group"))).thenReturn(availabilitySetName);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        assertEquals(Map.of(
                AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME,
                AzureLoadBalancerMetadataView.getAvailabilitySetParam(443), availabilitySetName), parameters);
    }

    @Test
    public void testCollectLoadBalancerKeepsAvailabilitySetOfRenamedCloudbreakRule() {
        AuthenticatedContext ac = authenticatedContext();
        Map<String, LoadBalancingRule> rules = Map.of("knox-https", loadBalancingRule("knox-https", 443, "gateway-pool"));
        String availabilitySetName = "stackName-gateway-as";

        when(azureClient.getLoadBalancerRules(eq(RESOURCE_GROUP), eq(LB_NAME))).thenReturn(rules);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName))).thenReturn(Optional.of(mock(AvailabilitySet.class)));
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq("gateway"))).thenReturn(availabilitySetName);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        assertEquals(Map.of(
                AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME,
                AzureLoadBalancerMetadataView.getAvailabilitySetParam(443), availabilitySetName), parameters);
    }

    @Test
    public void testCollectLoadBalancerWithForeignRuleWithoutAvailabilitySet() {
        AuthenticatedContext ac = authenticatedContext();
        Map<String, LoadBalancingRule> rules = new LinkedHashMap<>();
        rules.put("aaa-foreign-21050", loadBalancingRule("aaa-foreign-21050", 21050, "custom-pool"));
        rules.put("port-443TCP-rule", loadBalancingRule("port-443TCP-rule", 443, "gateway-pool"));
        String availabilitySetName = "stackName-gateway-as";
        String foreignAvailabilitySetName = "stackName-custom-as";

        when(azureClient.getLoadBalancerRules(eq(RESOURCE_GROUP), eq(LB_NAME))).thenReturn(rules);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName))).thenReturn(Optional.of(mock(AvailabilitySet.class)));
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(foreignAvailabilitySetName))).thenReturn(Optional.empty());
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq("gateway"))).thenReturn(availabilitySetName);
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq("custom"))).thenReturn(foreignAvailabilitySetName);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        Map<String, Object> expectedParameters = new HashMap<>();
        expectedParameters.put(AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME);
        expectedParameters.put(AzureLoadBalancerMetadataView.getAvailabilitySetParam(443), availabilitySetName);
        expectedParameters.put(AzureLoadBalancerMetadataView.getAvailabilitySetParam(21050), null);
        assertEquals(expectedParameters, parameters);
    }

    @Test
    public void testCollectLoadBalancerWithLegacyAddressPool() {
        AuthenticatedContext ac = authenticatedContext();
        Map<String, LoadBalancingRule> rules = Map.of("port-443-rule", loadBalancingRule("port-443-rule", 443, "address-pool"));
        String availabilitySetName = "stackName-address-as";

        when(azureClient.getLoadBalancerRules(eq(RESOURCE_GROUP), eq(LB_NAME))).thenReturn(rules);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName))).thenReturn(Optional.of(mock(AvailabilitySet.class)));
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq("address"))).thenReturn(availabilitySetName);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        assertEquals(Map.of(
                AzureLoadBalancerMetadataView.LOADBALANCER_NAME, LB_NAME,
                AzureLoadBalancerMetadataView.getAvailabilitySetParam(443), availabilitySetName), parameters);
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext context = CloudContext.Builder.builder()
                .withId(5L)
                .withName(STACK_NAME)
                .withCrn("crn")
                .withPlatform("platform")
                .withVariant("variant")
                .withLocation(location)
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential credential = new CloudCredential("crn", null, null, "acc");
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);
        authenticatedContext.putParameter(AzureClient.class, azureClient);
        return authenticatedContext;
    }

    private Map<String, LoadBalancingRule> getLoadBalancingRules(int numPorts) {
        Map<String, LoadBalancingRule> rules = new HashMap<>();
        for (int i = 0; i < numPorts; i++) {
            String ruleName = AzureLoadBalancerResourceNames.ruleName(i, "TCP");
            LoadBalancingRule rule = getLoadBalancingRule(i, ruleName, String.format(GROUP_PATTERN, i, POOL_SUFFIX));
            rules.put(String.valueOf(i), rule);
            // creating duplicate rules for "-gateway" suffix to emulate data returned from Azure in case of multiple frontends
            LoadBalancingRule rule2 = getLoadBalancingRule(i, ruleName + GATEWAY_SUFFIX, String.format(BACKEND_GROUP_PATTERN, i, POOL_SUFFIX, GATEWAY_SUFFIX));
            rules.put(String.valueOf(numPorts + i), rule2);
        }
        return rules;
    }

    private static LoadBalancingRule loadBalancingRule(String ruleName, int backendPort, String poolName) {
        LoadBalancingRule rule = mock(LoadBalancingRule.class);
        LoadBalancerBackend backend = mock(LoadBalancerBackend.class);
        BackendAddressPoolInner inner = mock(BackendAddressPoolInner.class);
        lenient().when(rule.name()).thenReturn(ruleName);
        lenient().when(inner.name()).thenReturn(poolName);
        lenient().when(backend.innerModel()).thenReturn(inner);
        lenient().when(rule.backend()).thenReturn(backend);
        lenient().when(rule.backendPort()).thenReturn(backendPort);
        return rule;
    }

    @NotNull
    private static LoadBalancingRule getLoadBalancingRule(int i, String ruleName, String poolName) {
        LoadBalancingRule rule = mock(LoadBalancingRule.class);
        LoadBalancerBackend backend = mock(LoadBalancerBackend.class);
        BackendAddressPoolInner inner = mock(BackendAddressPoolInner.class);
        when(rule.name()).thenReturn(ruleName);
        lenient().when(inner.name()).thenReturn(poolName);
        lenient().when(backend.innerModel()).thenReturn(inner);
        lenient().when(rule.backend()).thenReturn(backend);
        lenient().when(rule.backendPort()).thenReturn(i);
        return rule;
    }
}
