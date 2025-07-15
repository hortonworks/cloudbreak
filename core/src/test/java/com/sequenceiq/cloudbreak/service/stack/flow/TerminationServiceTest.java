package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;

@ExtendWith(MockitoExtension.class)
class TerminationServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private FreeIpaCleanupService freeIpaCleanupService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private FinalizationCleanUpService cleanUpService;

    @Mock
    private StackEncryptionService stackEncryptionService;

    @Mock
    private Clock clock;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private StackStatusService stackStatusService;

    @InjectMocks
    private TerminationService underTest;

    @Test
    void testFinalizeRecoveryFroInstancesWithoutInstanceIds() {
        StackDto stack = mock(StackDto.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);
        InstanceMetaData im3 = mock(InstanceMetaData.class);
        InstanceMetaData im4 = mock(InstanceMetaData.class);

        when(stackDtoService.getById(1L)).thenReturn(stack);
        when(stack.getNotTerminatedInstanceMetaData()).thenReturn(List.of(im1, im2, im3, im4));

        underTest.finalizeRecoveryTeardown(1L);

        verify(freeIpaCleanupService).cleanupButIp(stack);
        verify(instanceMetaDataService).deleteAllByInstanceIds(List.of(0L, 0L, 0L, 0L));
        verifyNoInteractions(instanceGroupService);
    }

    @Test
    void testFinalizeTermination() throws TransactionService.TransactionExecutionException {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(stackService.save(stack)).thenReturn(stack);
        doNothing().when(stackStatusService).cleanupStatus(anyLong(), any());
        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get()).when(transactionService).required(any(Supplier.class));


        underTest.finalizeTermination(STACK_ID, true);

        verify(freeIpaCleanupService).cleanupButIp(stack);
        verify(stackEncryptionService).deleteStackEncryption(STACK_ID);
        verify(cleanUpService).cleanUpStructuredEventsForStack(STACK_ID);
        verify(cleanUpService).detachClusterComponentRelatedAuditEntries(STACK_ID);
        verify(stackService).save(stack);
        verify(stackStatusService).cleanupStatus(anyLong(), eq(DELETE_COMPLETED));

    }

}