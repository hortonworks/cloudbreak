package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.network.instancegroup.InstanceGroupNetworkService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class GcpSubnetIdFixPatchServiceTest {

    private static final Long STACK_ID = 123L;

    private static final String ENVIRONMENT_CRN = "environment-crn";

    private static final String REGION = "us-west2";

    private static final String PLATFORM_VARIANT = "GCP";

    private static final String NETWORK_ID = "network-1";

    private static final String SHARED_PROJECT_ID = "shared-project-1";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private NetworkService networkService;

    @Mock
    private InstanceGroupNetworkService instanceGroupNetworkService;

    @InjectMocks
    private GcpSubnetIdFixPatchService underTest;

    @Captor
    private ArgumentCaptor<List<InstanceGroupNetwork>> instanceGroupNetworkCaptor;

    @Captor
    private ArgumentCaptor<List<Resource>> resourceCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> filterCaptor;

    @Test
    void testGetStackPatchType() {
        assertEquals(StackPatchType.GCP_SUBNET_ID_FIX, underTest.getStackPatchType());
    }

    static Stream<Arguments> testIsAffectedParameters() {
        return Stream.of(
                Arguments.of("AWS", null, null, null, null,
                        false),
                Arguments.of("AZURE", null, null, null, null,
                        false),
                Arguments.of("GCP", Set.of("sn-1", "sn-2", "sn-3"), "sn-1",
                        Map.of("master", List.of("sn-1"), "compute", List.of("sn-1")), List.of("sn-1"),
                        false),
                Arguments.of("GCP", Set.of("sn-1", "sn-2", "sn-3"), "sn-4",
                        Map.of("master", List.of("sn-1"), "compute", List.of("sn-1")), List.of("sn-1"),
                        true),
                Arguments.of("GCP", Set.of("sn-1", "sn-2", "sn-3"), "sn-1",
                        Map.of("master", List.of("sn-4"), "compute", List.of("sn-1")), List.of("sn-1"),
                        true),
                Arguments.of("GCP", Set.of("sn-1", "sn-2", "sn-3"), "sn-1",
                        Map.of("master", List.of("sn-1"), "compute", List.of("sn-4")), List.of("sn-1"),
                        true),
                Arguments.of("GCP", Set.of("sn-1", "sn-2", "sn-3"), "sn-1",
                        Map.of("master", List.of("sn-1"), "compute", List.of("sn-1")), List.of("sn-4"),
                        true)
        );
    }

    @MethodSource("testIsAffectedParameters")
    @ParameterizedTest
    void testIsAffected(String cloudPlatform, Set<String> environmentSubnetIds, String stackSubnetId, Map<String, List<String>> instanceGroupSubnetIds,
            List<String> subnetResourceIds, boolean expected) {
        Stack stack = getStackForIsAffected(cloudPlatform, environmentSubnetIds, stackSubnetId, instanceGroupSubnetIds, subnetResourceIds);

        assertEquals(expected, underTest.isAffected(stack));
    }

    static Stream<Arguments> testDoApplyParameters() {
        return Stream.of(
                Arguments.of(Set.of("sn-1", "sn-2", "sn-3"), "sn-1-provider-id",
                        Map.of("master", List.of("sn-1"), "compute", List.of("sn-1")), 0,
                        List.of("sn-1"), 0,
                        true, false, false),
                Arguments.of(Set.of("sn-1", "sn-2", "sn-3"), "sn-1",
                        Map.of("master", List.of("sn-1-provider-id"), "compute", List.of("sn-1")), 1,
                        List.of("sn-1"), 0,
                        false, true, false),
                Arguments.of(Set.of("sn-1", "sn-2", "sn-3"), "sn-1",
                        Map.of("master", List.of("sn-1"), "compute", List.of("sn-1-provider-id")), 1,
                        List.of("sn-1"), 0,
                        false, true, false),
                Arguments.of(Set.of("sn-1", "sn-2", "sn-3"), "sn-1",
                        Map.of("master", List.of("sn-1"), "compute", List.of("sn-1")), 0,
                        List.of("sn-1-provider-id", "sn-2"), 1,
                        false, false, true),
                Arguments.of(Set.of("sn-1", "sn-2", "sn-3"), "sn-1-provider-id",
                        Map.of("master", List.of("sn-1-provider-id"), "compute", List.of("sn-1-provider-id")), 2,
                        List.of("sn-1-provider-id", "sn-2-provider-id"), 2,
                        true, true, true)
        );
    }

    @MethodSource("testDoApplyParameters")
    @ParameterizedTest
    void testDoApply(Set<String> environmentSubnetIds, String stackSubnetId,
            Map<String, List<String>> instanceGroupSubnetIds, int expectedInstanceGroupUpdates,
            List<String> subnetResourceIds, int expectedResourceUpdates,
            boolean verifyNetworkUpdate, boolean verifyInstanceGroupNetworkUpdate, boolean verifyResourceUpdate)
            throws ExistingStackPatchApplyException, TransactionService.TransactionExecutionException {
        Stack stack = getStackForDoApply(environmentSubnetIds, stackSubnetId, instanceGroupSubnetIds, subnetResourceIds);
        ExtendedCloudCredential extendedCloudCredential = mock();
        when(credentialClientService.getExtendedCloudCredential(ENVIRONMENT_CRN)).thenReturn(extendedCloudCredential);
        CloudNetworks cloudNetworks = mock();
        CloudNetwork cloudNetwork = mock();
        when(cloudParameterService.getCloudNetworks(eq(extendedCloudCredential), eq(REGION), eq(PLATFORM_VARIANT), anyMap())).thenReturn(cloudNetworks);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(Map.of(REGION, Set.of(cloudNetwork)));
        Set<CloudSubnet> cloudSubnets = getCloudSubnets(environmentSubnetIds);
        when(cloudNetwork.getSubnetsMeta()).thenReturn(cloudSubnets);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionService).required(any(Runnable.class));

        assertTrue(underTest.doApply(stack));

        verify(cloudParameterService).getCloudNetworks(eq(extendedCloudCredential), eq(REGION), eq(PLATFORM_VARIANT), filterCaptor.capture());
        Map<String, String> capturedFilter = filterCaptor.getValue();
        assertThat(capturedFilter).containsExactlyInAnyOrderEntriesOf(Map.of(
                "networkId", NETWORK_ID,
                "sharedProjectId", SHARED_PROJECT_ID,
                "subnetIds", environmentSubnetIds.stream().collect(Collectors.joining(","))
        ));
        if (verifyNetworkUpdate) {
            verify(networkService).savePure(stack.getNetwork());
        } else {
            verifyNoInteractions(networkService);
        }
        verify(instanceGroupNetworkService).saveAll(instanceGroupNetworkCaptor.capture());
        List<InstanceGroupNetwork> capturedInstanceGroupNetworks = instanceGroupNetworkCaptor.getValue();
        if (verifyInstanceGroupNetworkUpdate) {
            assertThat(capturedInstanceGroupNetworks).hasSize(expectedInstanceGroupUpdates);
        } else {
            assertThat(capturedInstanceGroupNetworks).isEmpty();
        }
        verify(resourceService).saveAll(resourceCaptor.capture());
        List<Resource> capturedResources = resourceCaptor.getValue();
        if (verifyResourceUpdate) {
            assertThat(capturedResources).hasSize(expectedResourceUpdates);
        } else {
            assertThat(capturedResources).isEmpty();
        }
    }

    @Test
    void testDoApplyWhenTransactionFails() throws ExistingStackPatchApplyException, TransactionService.TransactionExecutionException {
        Stack stack = getStackForDoApply(Set.of(), "sn-1", Map.of(), List.of());
        ExtendedCloudCredential extendedCloudCredential = mock();
        when(credentialClientService.getExtendedCloudCredential(ENVIRONMENT_CRN)).thenReturn(extendedCloudCredential);
        CloudNetworks cloudNetworks = mock();
        CloudNetwork cloudNetwork = mock();
        when(cloudParameterService.getCloudNetworks(eq(extendedCloudCredential), eq(REGION), eq(PLATFORM_VARIANT), anyMap())).thenReturn(cloudNetworks);
        when(cloudNetworks.getCloudNetworkResponses()).thenReturn(Map.of(REGION, Set.of(cloudNetwork)));
        when(cloudNetwork.getSubnetsMeta()).thenReturn(getCloudSubnets(Set.of()));
        doThrow(TransactionService.TransactionExecutionException.class).when(transactionService).required(any(Runnable.class));

        assertFalse(underTest.doApply(stack));
    }

    private Stack getStackForIsAffected(String cloudPlatform, Set<String> environmentSubnetIds, String stackSubnetId,
            Map<String, List<String>> instanceGroupSubnetIds, List<String> subnetResourceIds) {
        Stack stack = mock();
        when(stack.cloudPlatform()).thenReturn(cloudPlatform);
        if (CloudPlatform.GCP.name().equalsIgnoreCase(cloudPlatform)) {
            Map<String, String> stackNetworkAttributes = Map.of(
                    "subnetId", stackSubnetId
            );
            setupMocksForStack(stack, environmentSubnetIds, stackNetworkAttributes, instanceGroupSubnetIds, subnetResourceIds);
        }
        return stack;
    }

    private Stack getStackForDoApply(Set<String> environmentSubnetIds, String stackSubnetId, Map<String, List<String>> instanceGroupSubnetIds,
            List<String> subnetResourceIds) {
        Stack stack = mock();
        when(stack.getRegion()).thenReturn(REGION);
        when(stack.getPlatformVariant()).thenReturn(PLATFORM_VARIANT);
        Network network = mock();
        Map<String, String> stackNetworkAttributes = Map.of(
                "subnetId", stackSubnetId,
                "networkId", NETWORK_ID,
                "sharedProjectId", SHARED_PROJECT_ID
        );
        setupMocksForStack(stack, environmentSubnetIds, stackNetworkAttributes, instanceGroupSubnetIds, subnetResourceIds);
        return stack;
    }

    private void setupMocksForStack(Stack stack, Set<String> environmentSubnetIds, Map<String, String> stackNetworkAttributes,
            Map<String, List<String>> instanceGroupSubnetIds, List<String> subnetResourceIds) {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);

        DetailedEnvironmentResponse environment = mock();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        EnvironmentNetworkResponse environmentNetwork = mock();
        when(environment.getNetwork()).thenReturn(environmentNetwork);
        when(environmentNetwork.getSubnetIds()).thenReturn(environmentSubnetIds);

        Network network = mock();
        when(stack.getNetwork()).thenReturn(network);
        when(network.getAttributes()).thenReturn(new Json(stackNetworkAttributes));

        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroupSubnetIds.forEach((igName, subnetIds) -> {
            InstanceGroup instanceGroup = mock();
            when(instanceGroup.getGroupName()).thenReturn(igName);
            InstanceGroupNetwork instanceGroupNetwork = mock();
            when(instanceGroup.getInstanceGroupNetwork()).thenReturn(instanceGroupNetwork);
            when(instanceGroupNetwork.getAttributes()).thenReturn(new Json(Map.of("subnetIds", subnetIds)));
            instanceGroups.add(instanceGroup);
        });
        when(stack.getInstanceGroups()).thenReturn(instanceGroups);

        List<Resource> subnetResources = subnetResourceIds.stream().map(id -> {
            Resource resource = mock();
            when(resource.getResourceName()).thenReturn(id);
            return resource;
        }).toList();
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.GCP_SUBNET)).thenReturn(subnetResources);
    }

    private Set<CloudSubnet> getCloudSubnets(Set<String> subnetIds) {
        Set<CloudSubnet> cloudSubnets = new HashSet<>();
        for (String subnetId : subnetIds) {
            CloudSubnet cloudSubnet = mock();
            when(cloudSubnet.getParameter("providerSideId", String.class)).thenReturn(subnetId + "-provider-id");
            when(cloudSubnet.getId()).thenReturn(subnetId);
            cloudSubnets.add(cloudSubnet);
        }
        return cloudSubnets;
    }
}
