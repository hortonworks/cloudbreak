package com.sequenceiq.freeipa.flow.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.flow.core.FlowEventContext;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitState;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeState;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitState;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationFlowEventChainFactoryTest {

    private static final Long STACK_ID = 1L;

    private static final String ENV_CRN = "env-crn";

    private static final String USER_CRN = "user-crn";

    private static final String ACCOUNT_ID = "accountId";

    private static final String OPERATION_ID = "op-1";

    private static final String PRIMARY_GW = "i-pgw";

    private static final String NON_PGW_1 = "i-non-pgw-1";

    private static final String NON_PGW_2 = "i-non-pgw-2";

    private static final List<String> INSTANCE_IDS = List.of(PRIMARY_GW, NON_PGW_1, NON_PGW_2);

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private StackService stackService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private OperationService operationService;

    @Mock
    private FreeipaJobService freeipaJobService;

    @InjectMocks
    private MultiAzMigrationFlowEventChainFactory underTest;

    static Stream<Arguments> testCreateFlowTriggerEventQueueArguments() {
        return Stream.of(
                Arguments.of(true, Set.of(PRIMARY_GW, NON_PGW_1, NON_PGW_2)),
                Arguments.of(true, Set.of(PRIMARY_GW, NON_PGW_1)),
                Arguments.of(true, Set.of(PRIMARY_GW)),
                Arguments.of(false, Set.of(PRIMARY_GW, NON_PGW_1, NON_PGW_2)),
                Arguments.of(false, Set.of(PRIMARY_GW, NON_PGW_1)),
                Arguments.of(false, Set.of(PRIMARY_GW))
        );
    }

    @MethodSource("testCreateFlowTriggerEventQueueArguments")
    @ParameterizedTest
    void testCreateFlowTriggerEventQueueWhenVariantMigrationNeeded(boolean variantMigrationNeeded, Set<String> instanceIds) {
        when(instanceGroupService.findGroupNamesByStackId(STACK_ID)).thenReturn(Set.of("master"));
        MultiAzMigrationEvent event = new MultiAzMigrationEvent(
                FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT,
                STACK_ID,
                OPERATION_ID,
                variantMigrationNeeded ? AwsConstants.AwsVariant.AWS_VARIANT.variant() : AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(),
                AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(),
                new HashSet<>(instanceIds),
                PRIMARY_GW);

        FlowTriggerEventQueue queue = underTest.createFlowTriggerEventQueue(event);

        assertThat(queue.getQueue()).hasSize(getExpectedFlowCount(variantMigrationNeeded, instanceIds));

        List<UpscaleEvent> upscaleEvents = queue.getQueue().stream()
                .filter(UpscaleEvent.class::isInstance)
                .map(UpscaleEvent.class::cast)
                .toList();
        assertThat(upscaleEvents).hasSize(variantMigrationNeeded ? instanceIds.size() : instanceIds.size() - 1);
        assertThat(upscaleEvents).extracting(UpscaleEvent::getInstanceIdsBeingReplaced).allMatch(ids -> ids.size() == 1);
        assertThat(upscaleEvents).flatExtracting(UpscaleEvent::getInstanceIdsBeingReplaced)
                .containsExactlyInAnyOrderElementsOf(INSTANCE_IDS.subList(variantMigrationNeeded ? 0 : 1, instanceIds.size()));

        List<DownscaleEvent> downscaleEvents = queue.getQueue().stream()
                .filter(DownscaleEvent.class::isInstance)
                .map(DownscaleEvent.class::cast)
                .toList();
        assertThat(downscaleEvents).hasSize(variantMigrationNeeded ? instanceIds.size() : instanceIds.size() - 1);
        assertThat(downscaleEvents).extracting(DownscaleEvent::getInstanceIds).allMatch(ids -> ids.size() == 1);
        assertThat(downscaleEvents).flatExtracting(DownscaleEvent::getInstanceIds)
                .containsExactlyInAnyOrderElementsOf(INSTANCE_IDS.subList(variantMigrationNeeded ? 0 : 1, instanceIds.size()));
    }

    private static int getExpectedFlowCount(boolean variantMigrationNeeded, Set<String> instanceIds) {
        return switch (instanceIds.size()) {
            case 1 -> variantMigrationNeeded ? 9 : 4;
            case 2 -> variantMigrationNeeded ? 13 : 6;
            case 3 -> variantMigrationNeeded ? 15 : 8;
            default -> 0;
        };
    }

    @Test
    void testOnFlowChainFailure() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setAccountId(ACCOUNT_ID);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        Operation operation = new Operation();
        operation.setOperationId(OPERATION_ID);
        operation.setStatus(OperationState.RUNNING);
        when(operationService.getLatestOperationForEnvironmentCrnAndOperationType(ENV_CRN, OperationType.MIGRATE_TO_MULTI_AZ)).thenReturn(operation);
        FlowEventContext flowEventContext = mock();
        when(flowEventContext.getResourceId()).thenReturn(STACK_ID);
        when(flowEventContext.getFlowTriggerUserCrn()).thenReturn(USER_CRN);

        underTest.onFlowChainFailure(flowEventContext);

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.MULTI_AZ_MIGRATION_FAILED,
                "FreeIPA multi-AZ migration failed in one of the intermediate flows.");
        verify(eventSenderService).sendEventAndNotification(stack, USER_CRN, ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_FAILED,
                List.of("FreeIPA multi-AZ migration failed in one of the intermediate flows."));
        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID, "FreeIPA multi-AZ migration failed in one of the intermediate flows.");
        verify(freeipaJobService).schedule(STACK_ID);
    }

    static Stream<Arguments> testGetUseCaseForFlowStateArguments() {
        return Stream.of(
                Arguments.of(FlowChainInitState.INIT_STATE, UsageProto.CDPFreeIPAStatus.Value.MULTI_AZ_MIGRATION_STARTED),
                Arguments.of(FlowChainFinalizeState.FLOWCHAIN_FINALIZE_FINISHED_STATE, UsageProto.CDPFreeIPAStatus.Value.MULTI_AZ_MIGRATION_FINISHED),
                Arguments.of(MultiAzMigrationInitState.MULTI_AZ_MIGRATION_INIT_FAILED_STATE, UsageProto.CDPFreeIPAStatus.Value.MULTI_AZ_MIGRATION_FAILED),
                Arguments.of(MultiAzMigrationFinalizeState.MULTI_AZ_MIGRATION_FINALIZE_FAILED_STATE,
                        UsageProto.CDPFreeIPAStatus.Value.MULTI_AZ_MIGRATION_FAILED),
                Arguments.of(UpscaleState.UPSCALE_ADD_INSTANCES_STATE, UsageProto.CDPFreeIPAStatus.Value.UNSET)
        );
    }

    @MethodSource("testGetUseCaseForFlowStateArguments")
    @ParameterizedTest
    void testGetUseCaseForFlowState(Enum<? extends FlowState> flowState, UsageProto.CDPFreeIPAStatus.Value expected) {
        assertEquals(expected, underTest.getUseCaseForFlowState(flowState));
    }
}
