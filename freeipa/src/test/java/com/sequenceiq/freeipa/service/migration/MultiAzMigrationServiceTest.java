package com.sequenceiq.freeipa.service.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationServiceTest {

    private static final String ACCOUNT_ID = "account-id";

    private static final String ENV_CRN = "env-crn";

    private static final String OPERATION_ID = "op-id";

    private static final String PRIMARY_GW_INSTANCE_ID = "i-primary";

    private static final Long STACK_ID = 42L;

    @Mock
    private OperationService operationService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @InjectMocks
    private MultiAzMigrationService underTest;

    private Stack stack;

    private Operation runningOperation;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setPlatformvariant(CloudConstants.AWS);

        InstanceMetaData primary = instance(PRIMARY_GW_INSTANCE_ID, InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData secondary = instance("i-secondary", InstanceMetadataType.GATEWAY);
        InstanceGroup ig = new InstanceGroup();
        ig.setInstanceMetaData(Set.of(primary, secondary));
        stack.setInstanceGroups(Set.of(ig));

        runningOperation = new Operation();
        runningOperation.setOperationId(OPERATION_ID);
        runningOperation.setStatus(OperationState.RUNNING);
    }

    @Test
    void triggerMultiAzMigrationFailsOperationWhenFlowManagerThrows() {
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.MIGRATE_TO_MULTI_AZ), any(), any())).thenReturn(runningOperation);
        RuntimeException notifyFailure = new RuntimeException("flow manager went bang");
        when(flowManager.notify(eq(FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT), any())).thenThrow(notifyFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> underTest.triggerMultiAzMigration(ENV_CRN, ACCOUNT_ID, stack));

        assertEquals(notifyFailure, thrown);
        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID, "Could not start FreeIPA multi-AZ migration flow: flow manager went bang");
    }

    @Test
    void triggerMultiAzMigrationThrowsFailOperationExceptionWithOriginalSuppressedWhenFailOperationThrows() {
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.MIGRATE_TO_MULTI_AZ), any(), any())).thenReturn(runningOperation);
        RuntimeException notifyFailure = new RuntimeException("flow manager went bang");
        when(flowManager.notify(eq(FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT), any())).thenThrow(notifyFailure);
        RuntimeException failOperationFailure = new RuntimeException("DB hiccup");
        doThrow(failOperationFailure).when(operationService).failOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), anyString());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> underTest.triggerMultiAzMigration(ENV_CRN, ACCOUNT_ID, stack));

        assertEquals(failOperationFailure, thrown);
        assertTrue(Arrays.asList(thrown.getSuppressed()).contains(notifyFailure),
                "Original notify failure must be attached as a suppressed exception on the failOperation exception.");
    }

    @Test
    void triggerMultiAzMigrationReturnsResponseOnHappyPath() {
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.MIGRATE_TO_MULTI_AZ), any(), any())).thenReturn(runningOperation);
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, "fc-1");
        when(flowManager.notify(eq(FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT), any())).thenReturn(flowId);

        FreeIpaMultiAzMigrationV1Response response = underTest.triggerMultiAzMigration(ENV_CRN, ACCOUNT_ID, stack);

        assertEquals(OPERATION_ID, response.getOperationId());
        assertEquals(flowId, response.getFlowIdentifier());
        verify(operationService, never()).failOperation(anyString(), anyString(), anyString());
    }

    @Test
    void triggerMultiAzMigrationDoesNotRequireVariantMigrationWhenStackAlreadyOnNativeAws() {
        stack.setPlatformvariant(CloudConstants.AWS_NATIVE);
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.MIGRATE_TO_MULTI_AZ), any(), any())).thenReturn(runningOperation);
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, "fc-2");
        when(flowManager.notify(eq(FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT), any())).thenReturn(flowId);

        FreeIpaMultiAzMigrationV1Response response = underTest.triggerMultiAzMigration(ENV_CRN, ACCOUNT_ID, stack);

        ArgumentCaptor<MultiAzMigrationEvent> eventCaptor = ArgumentCaptor.forClass(MultiAzMigrationEvent.class);
        verify(flowManager).notify(eq(FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT), eventCaptor.capture());
        MultiAzMigrationEvent event = eventCaptor.getValue();
        assertEquals(CloudConstants.AWS_NATIVE, event.getSourceVariant().getValue());
        assertEquals(CloudConstants.AWS_NATIVE, event.getTargetVariant().getValue());
        assertFalse(event.variantMigrationNeeded(), "Variant migration must not be needed when stack is already on AWS_NATIVE.");
        assertFalse(event.shouldRecreatePrimaryGw(), "Primary gateway must not be recreated when variant migration is not needed.");
        assertTrue(event.getNonPrimaryGwInstanceIdsToRecreate().stream().noneMatch(PRIMARY_GW_INSTANCE_ID::equals),
                "Recreate set should not contain the primary gateway when variant migration is not needed.");
        assertEquals(OPERATION_ID, response.getOperationId());
    }

    @Test
    void triggerMultiAzMigrationThrowsWhenOperationStartReturnsNonRunning() {
        Operation rejected = new Operation();
        rejected.setStatus(OperationState.REJECTED);
        rejected.setError("already running");
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.MIGRATE_TO_MULTI_AZ), any(), any())).thenReturn(rejected);

        assertThrows(BadRequestException.class, () -> underTest.triggerMultiAzMigration(ENV_CRN, ACCOUNT_ID, stack));
    }

    private static InstanceMetaData instance(String id, InstanceMetadataType type) {
        InstanceMetaData md = new InstanceMetaData();
        md.setInstanceId(id);
        md.setInstanceMetadataType(type);
        return md;
    }
}
