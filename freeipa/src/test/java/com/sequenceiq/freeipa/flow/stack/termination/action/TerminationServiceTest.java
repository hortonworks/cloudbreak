package com.sequenceiq.freeipa.flow.stack.termination.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KeytabCleanupService;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.StructuredEventCleanupService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackStatusService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class TerminationServiceTest {

    private static final String STACK_NAME = "freeipa-cluster";

    private static final long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private Clock clock;

    @Mock
    private TransactionService transactionService;

    @Mock
    private KeytabCleanupService keytabCleanupService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FreeIpaRecipeService freeIpaRecipeService;

    @Mock
    private StructuredEventCleanupService mockStructuredEventCleanupService;

    @Mock
    private StackEncryptionService stackEncryptionService;

    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Mock
    private StackStatusService stackStatusService;

    @InjectMocks
    private TerminationService underTest;

    @Test
    void testFinalizeTerminationFroInstancesWithoutInstanceIds() {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);
        InstanceMetaData im3 = mock(InstanceMetaData.class);
        InstanceMetaData im4 = mock(InstanceMetaData.class);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getAllInstanceMetaDataList()).thenReturn(List.of(im1, im2, im3, im4));
        when(im1.isTerminated()).thenReturn(true);
        when(im2.isTerminated()).thenReturn(false);
        when(im3.isTerminated()).thenReturn(false);
        when(im4.isTerminated()).thenReturn(false);
        when(im2.getInstanceId()).thenReturn(null);
        when(im3.getInstanceId()).thenReturn("i-3");
        when(im4.getInstanceId()).thenReturn(null);
        when(clock.getCurrentTimeMillis()).thenReturn(1L);

        underTest.finalizeTerminationForInstancesWithoutInstanceIds(STACK_ID);

        verify(im2).setTerminationDate(any());
        verify(im2).setInstanceStatus(eq(InstanceStatus.TERMINATED));
        verify(im4).setTerminationDate(any());
        verify(im4).setInstanceStatus(eq(InstanceStatus.TERMINATED));
        verify(instanceMetaDataService).saveAll(any());
    }

    @Test
    void testRequestDeletionForInstances() {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);
        InstanceMetaData im3 = mock(InstanceMetaData.class);
        InstanceMetaData im4 = mock(InstanceMetaData.class);

        when(stack.getAllInstanceMetaDataList()).thenReturn(List.of(im1, im2, im3, im4));
        when(im1.isTerminated()).thenReturn(true);
        when(im2.isTerminated()).thenReturn(false);
        when(im3.isTerminated()).thenReturn(false);
        when(im4.isTerminated()).thenReturn(false);
        when(im2.getInstanceId()).thenReturn("i-2");
        when(im3.getInstanceId()).thenReturn("i-3");
        when(im4.getInstanceId()).thenReturn("i-4");

        underTest.requestDeletionForInstances(stack, List.of("i-3"));

        verify(im1, never()).setInstanceStatus(any());
        verify(im2, never()).setInstanceStatus(any());
        verify(im3).setInstanceStatus(eq(InstanceStatus.DELETE_REQUESTED));
        verify(im4, never()).setInstanceStatus(any());
        verify(instanceMetaDataService).saveAll(any());
    }

    @Test
    void testfinalizeTerminationTransaction() {
        Stack stack = mock(Stack.class);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(stack.getId()).thenReturn(STACK_ID);
        doNothing().when(stackStatusService).cleanupByPreservedStatus(anyLong(), any());

        underTest.finalizeTerminationTransaction(STACK_ID);

        verify(stack).setName(anyString());
        verify(stack).setTerminated(any());
        verify(keytabCleanupService).cleanupByEnvironment(any(), any());
        verify(stackUpdater).updateStackStatus(eq(stack),
                eq(DetailedStackStatus.DELETE_COMPLETED),
                eq("Stack was terminated successfully."));
        verify(stackService).save(eq(stack));
        verify(instanceMetaDataService, never()).save(any());
        verify(freeIpaRecipeService).deleteRecipes(STACK_ID);
        verify(stackEncryptionService).deleteStackEncryption(STACK_ID);
        verify(freeIpaLoadBalancerService).delete(STACK_ID);
        verify(stackStatusService).cleanupByPreservedStatus(anyLong(), eq(DELETE_COMPLETED));
    }

}