package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.network.fluent.models.BackendAddressPoolInner;
import com.azure.resourcemanager.network.models.LoadBalancerBackend;
import com.azure.resourcemanager.network.models.LoadBalancingRule;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
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
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), anyString())).thenReturn(mock(AvailabilitySet.class));
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
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), anyString())).thenReturn(mock(AvailabilitySet.class));
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
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName0))).thenReturn(mock(AvailabilitySet.class));
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName1))).thenReturn(null);
        when(azureClient.getAvailabilitySet(eq(RESOURCE_GROUP), eq(availabilitySetName2))).thenReturn(mock(AvailabilitySet.class));
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName0))).thenReturn(availabilitySetName0);
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName1))).thenReturn(availabilitySetName1);
        when(availabilitySetNameService.generateName(eq(STACK_NAME), eq(groupName2))).thenReturn(availabilitySetName2);

        Map<String, Object> parameters = underTest.getParameters(ac, RESOURCE_GROUP, LB_NAME);

        assertEquals(expectedParameters, parameters);
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
            LoadBalancingRule rule = getLoadBalancingRule(i, String.format(GROUP_PATTERN, i, POOL_SUFFIX));
            rules.put(String.valueOf(i), rule);
            // creating duplicate rules for "-gateway" suffix to emulate data returned from Azure in case of multiple frontends
            LoadBalancingRule rule2 = getLoadBalancingRule(i, String.format(BACKEND_GROUP_PATTERN, i, POOL_SUFFIX, GATEWAY_SUFFIX));
            rules.put(String.valueOf(numPorts + i), rule2);
        }
        return rules;
    }

    @NotNull
    private static LoadBalancingRule getLoadBalancingRule(int i, String name) {
        LoadBalancingRule rule = mock(LoadBalancingRule.class);
        LoadBalancerBackend backend = mock(LoadBalancerBackend.class);
        BackendAddressPoolInner inner = mock(BackendAddressPoolInner.class);
        when(rule.name()).thenReturn(name);
        lenient().when(inner.name()).thenReturn(name);
        lenient().when(backend.innerModel()).thenReturn(inner);
        lenient().when(rule.backend()).thenReturn(backend);
        lenient().when(rule.backendPort()).thenReturn(i);
        return rule;
    }
}
