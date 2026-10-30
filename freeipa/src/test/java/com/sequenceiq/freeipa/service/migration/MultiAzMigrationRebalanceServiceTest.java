package com.sequenceiq.freeipa.service.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationEvent;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationRebalanceServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String ENV_CRN = "env-crn";

    private static final String OPERATION_ID = "op-id";

    private static final String PRIMARY_GW = "i-pgw";

    private static final String NON_PGW_1 = "i-non-pgw-1";

    private static final String NON_PGW_2 = "i-non-pgw-2";

    private static final String AZ_1 = "az-1";

    private static final String AZ_2 = "az-2";

    private static final String AZ_3 = "az-3";

    private static final String AZ_4 = "az-4";

    private static final Set<String> TWO_AZS = Set.of(AZ_1, AZ_2);

    private static final Set<String> THREE_AZS = Set.of(AZ_1, AZ_2, AZ_3);

    private static final Set<String> FOUR_AZS = Set.of(AZ_1, AZ_2, AZ_3, AZ_4);

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private EnvironmentNetworkResponse network;

    @InjectMocks
    private MultiAzMigrationRebalanceService underTest;

    static Stream<Arguments> testGetNonPrimaryGwInstanceIdsToRecreateArguments() {
        Map<String, String> oneNodeInOneAz = Map.of(PRIMARY_GW, AZ_1);
        Map<String, String> twoNodesInOneAz = Map.of(PRIMARY_GW, AZ_1, NON_PGW_1, AZ_1);
        Map<String, String> threeNodesInOneAz = Map.of(PRIMARY_GW, AZ_1, NON_PGW_1, AZ_1, NON_PGW_2, AZ_1);
        Map<String, String> twoNodesInTwoAzs = Map.of(PRIMARY_GW, AZ_1, NON_PGW_1, AZ_2);
        Map<String, String> threeNodesInTwoAzs = Map.of(PRIMARY_GW, AZ_1, NON_PGW_1, AZ_1, NON_PGW_2, AZ_2);
        Map<String, String> threeNodesInThreeAzs = Map.of(PRIMARY_GW, AZ_1, NON_PGW_1, AZ_2, NON_PGW_2, AZ_3);
        return Stream.of(
                Arguments.of("1 node in 1 AZ, 2 AZs", oneNodeInOneAz, TWO_AZS, Set.of()),
                Arguments.of("1 node in 1 AZ, 3 AZs", oneNodeInOneAz, THREE_AZS, Set.of()),
                Arguments.of("1 node in 1 AZ, 4 AZs", oneNodeInOneAz, FOUR_AZS, Set.of()),
                Arguments.of("2 nodes in 1 AZ, 2 AZs", twoNodesInOneAz, TWO_AZS, Set.of(NON_PGW_1)),
                Arguments.of("2 nodes in 1 AZ, 3 AZs", twoNodesInOneAz, THREE_AZS, Set.of(NON_PGW_1)),
                Arguments.of("2 nodes in 1 AZ, 4 AZs", twoNodesInOneAz, FOUR_AZS, Set.of(NON_PGW_1)),
                Arguments.of("3 nodes in 1 AZ, 2 AZs", threeNodesInOneAz, TWO_AZS, Set.of(NON_PGW_1)),
                Arguments.of("3 nodes in 1 AZ, 3 AZs", threeNodesInOneAz, THREE_AZS, Set.of(NON_PGW_1, NON_PGW_2)),
                Arguments.of("3 nodes in 1 AZ, 4 AZs", threeNodesInOneAz, FOUR_AZS, Set.of(NON_PGW_1, NON_PGW_2)),

                Arguments.of("2 nodes evenly distributed, 2 AZs", twoNodesInTwoAzs, TWO_AZS, Set.of()),
                Arguments.of("2 nodes evenly distributed, 3 AZs", twoNodesInTwoAzs, THREE_AZS, Set.of()),
                Arguments.of("2 nodes evenly distributed, 4 AZs", twoNodesInTwoAzs, FOUR_AZS, Set.of()),
                Arguments.of("3 nodes evenly distributed, 2 AZs", threeNodesInTwoAzs, TWO_AZS, Set.of()),
                Arguments.of("3 nodes evenly distributed with primary gateway alone, 2 AZs",
                        Map.of(PRIMARY_GW, AZ_2, NON_PGW_1, AZ_1, NON_PGW_2, AZ_1), TWO_AZS, Set.of()),
                Arguments.of("3 nodes evenly distributed, 3 AZs", threeNodesInThreeAzs, THREE_AZS, Set.of()),
                Arguments.of("3 nodes evenly distributed, 4 AZs", threeNodesInThreeAzs, FOUR_AZS, Set.of()),

                Arguments.of("3 nodes split 2:1, 3 AZs", threeNodesInTwoAzs, THREE_AZS, Set.of(NON_PGW_1)),
                Arguments.of("node outside of the environment AZs", Map.of(PRIMARY_GW, AZ_1, NON_PGW_1, AZ_1, NON_PGW_2, AZ_3), TWO_AZS,
                        Set.of(NON_PGW_2)),
                Arguments.of("primary gateway outside of the environment AZs", Map.of(PRIMARY_GW, AZ_3, NON_PGW_1, AZ_1, NON_PGW_2, AZ_1), TWO_AZS,
                        Set.of(NON_PGW_1, NON_PGW_2)),
                Arguments.of("no environment AZs", threeNodesInOneAz, Set.of(), Set.of(NON_PGW_1, NON_PGW_2))
        );
    }

    @MethodSource("testGetNonPrimaryGwInstanceIdsToRecreateArguments")
    @ParameterizedTest(name = "{0}")
    void testGetNonPrimaryGwInstanceIdsToRecreate(String name, Map<String, String> instanceAzs, Set<String> environmentAzs, Set<String> expected) {
        mockStack(environmentAzs);
        Set<InstanceMetaData> instances = instanceAzs.entrySet().stream()
                .map(e -> instance(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
        when(instanceMetaDataService.getNotTerminatedByInstanceIds(STACK_ID, instanceAzs.keySet())).thenReturn(instances);

        Set<String> result = underTest.getNonPrimaryGwInstanceIdsToRecreate(event(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(),
                instanceAzs.keySet().toArray(String[]::new)));

        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void testGetNonPrimaryGwInstanceIdsToRecreateWhenAvailabilityZoneIsMissing() {
        mockStack(Set.of(AZ_1, AZ_2));
        when(instanceMetaDataService.getNotTerminatedByInstanceIds(STACK_ID, Set.of(PRIMARY_GW, NON_PGW_1, NON_PGW_2)))
                .thenReturn(Set.of(instance(PRIMARY_GW, AZ_1), instance(NON_PGW_1, AZ_1), instance(NON_PGW_2, null)));

        Set<String> result = underTest.getNonPrimaryGwInstanceIdsToRecreate(event(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(),
                PRIMARY_GW, NON_PGW_1, NON_PGW_2));

        assertThat(result).containsExactly(NON_PGW_2);
    }

    @Test
    void testGetNonPrimaryGwInstanceIdsToRecreateWhenAvailabilityZoneOfPrimaryGwIsMissing() {
        mockStack(Set.of(AZ_1, AZ_2));
        when(instanceMetaDataService.getNotTerminatedByInstanceIds(STACK_ID, Set.of(PRIMARY_GW, NON_PGW_1, NON_PGW_2)))
                .thenReturn(Set.of(instance(PRIMARY_GW, null), instance(NON_PGW_1, AZ_1), instance(NON_PGW_2, AZ_1)));

        Set<String> result = underTest.getNonPrimaryGwInstanceIdsToRecreate(event(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(),
                PRIMARY_GW, NON_PGW_1, NON_PGW_2));

        assertThat(result).containsExactlyInAnyOrder(NON_PGW_1, NON_PGW_2);
    }

    @Test
    void testGetNonPrimaryGwInstanceIdsToRecreateWhenEnvironmentAzsContainNull() {
        mockStack(new HashSet<>(Arrays.asList(AZ_1, null)));
        when(instanceMetaDataService.getNotTerminatedByInstanceIds(STACK_ID, Set.of(PRIMARY_GW, NON_PGW_1, NON_PGW_2)))
                .thenReturn(Set.of(instance(PRIMARY_GW, AZ_1), instance(NON_PGW_1, AZ_1), instance(NON_PGW_2, AZ_1)));

        Set<String> result = underTest.getNonPrimaryGwInstanceIdsToRecreate(event(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(),
                PRIMARY_GW, NON_PGW_1, NON_PGW_2));

        assertThat(result).isEmpty();
    }

    @Test
    void testGetNonPrimaryGwInstanceIdsToRecreateWhenPrimaryGwIsRecreated() {
        Set<String> result = underTest.getNonPrimaryGwInstanceIdsToRecreate(event(AzureConstants.VARIANT, PRIMARY_GW, NON_PGW_1, NON_PGW_2));

        assertThat(result).containsExactlyInAnyOrder(NON_PGW_1, NON_PGW_2);
        verifyNoInteractions(stackService, instanceMetaDataService, cachedEnvironmentClientService);
    }

    private void mockStack(Set<String> environmentAzs) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setNetwork(network);
        when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(environment);
        when(network.getAvailabilityZones(CloudPlatform.AWS)).thenReturn(environmentAzs);
    }

    private static MultiAzMigrationEvent event(Variant variant, String... instanceIds) {
        return new MultiAzMigrationEvent(FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT, STACK_ID, OPERATION_ID, variant, variant,
                new HashSet<>(Arrays.asList(instanceIds)), PRIMARY_GW);
    }

    private static InstanceMetaData instance(String instanceId, String availabilityZone) {
        InstanceMetaData instance = new InstanceMetaData();
        instance.setInstanceId(instanceId);
        instance.setAvailabilityZone(availabilityZone);
        return instance;
    }
}
