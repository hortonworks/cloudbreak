package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateProperties;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeMetadata;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeProperty;
import com.sequenceiq.cloudbreak.message.StackStatusMessageTransformator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;

@ExtendWith(MockitoExtension.class)
public class StackUpdaterTest {

    private static final Long STACK_ID = 1L;

    private static final String STACK_CRN = "crn:cdp:freeipa:us-west-1:acc1:stack:12345";

    private static final String INITIAL_STATUS_REASON = "initial reason";

    private static final String NEW_STATUS_REASON = "new reason";

    private static final String TRANSFORMED_STATUS_REASON = "transformed new reason";

    private static final String RAW_NEW_STATUS_REASON = "raw new reason";

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetadataUpdateProperties instanceMetadataUpdateProperties;

    @Mock
    private StackStatusMessageTransformator stackStatusMessageTransformator;

    @Mock
    private ServiceStatusRawMessageTransformer serviceStatusRawMessageTransformer;

    @InjectMocks
    private StackUpdater underTest;

    private Stack stack;

    private StackStatus initialStackStatus;

    @Test
    void testUpdateImdsVersionIfMatchingVersion() {
        stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setSupportedImdsVersion("v2");
        when(stackService.getStackById(any())).thenReturn(stack);
        mockGetTypes();

        underTest.updateSupportedImdsVersion(1L, InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED);

        verify(stackService, times(0)).save(any());
    }

    @Test
    void testUpdateImdsVersion() {
        stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setSupportedImdsVersion("v1");
        when(stackService.getStackById(any())).thenReturn(stack);
        mockGetTypes();
        when(stackService.save(any())).thenReturn(stack);

        underTest.updateSupportedImdsVersion(1L, InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED);

        verify(stackService).save(any());
    }

    private void mockGetTypes() {
        InstanceMetadataUpdateTypeMetadata metadataV2 = new InstanceMetadataUpdateTypeMetadata("v2");
        InstanceMetadataUpdateTypeProperty propertyV2 = new InstanceMetadataUpdateTypeProperty("AWS", Map.of(AWS, metadataV2));
        when(instanceMetadataUpdateProperties.getTypes()).thenReturn(Map.of(InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED, propertyV2));
    }

    @BeforeEach
    void setUp() {
        // Initialize a mock Stack object for consistent testing
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setResourceCrn(STACK_CRN);
        stack.setCloudPlatform("AWS");

        // Set up initial StackStatus
        initialStackStatus = new StackStatus(stack, Status.AVAILABLE, INITIAL_STATUS_REASON, DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(initialStackStatus);

        // Mock common service behaviors
        lenient().when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        lenient().when(stackService.save(any(Stack.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(stackStatusMessageTransformator.transformMessage(anyString())).thenReturn(TRANSFORMED_STATUS_REASON);
        lenient().when(serviceStatusRawMessageTransformer.transformMessage(anyString(), any())).thenReturn(RAW_NEW_STATUS_REASON);
    }

    @Test
    void testUpdateStackStatusByIdStatusChanged() {
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.PROVISION_REQUESTED;
        Status newStatus = newDetailedStatus.getStatus();

        // Use mockStatic for InMemoryStateStore as it has static methods
        try (MockedStatic<InMemoryStateStore> mockedInMemoryStateStore = mockStatic(InMemoryStateStore.class)) {
            mockedInMemoryStateStore.when(() -> InMemoryStateStore.getStack(STACK_ID)).thenReturn(PollGroup.POLLABLE);

            Stack updatedStack = underTest.updateStackStatus(STACK_ID, newDetailedStatus, NEW_STATUS_REASON);

            // Verify stackService.getStackById was called
            verify(stackService, times(1)).getStackById(STACK_ID);
            // Verify message transformation
            verify(serviceStatusRawMessageTransformer, times(1)).transformMessage(eq(NEW_STATUS_REASON), any());
            verify(stackStatusMessageTransformator, times(1)).transformMessage(eq(RAW_NEW_STATUS_REASON));

            // Verify saveStackNewStatus logic
            ArgumentCaptor<Stack> stackCaptor = ArgumentCaptor.forClass(Stack.class);
            verify(stackService, times(1)).save(stackCaptor.capture());

            Stack savedStack = stackCaptor.getValue();
            assertNotNull(savedStack.getStackStatus());
            assertEquals(newStatus, savedStack.getStackStatus().getStatus());
            assertEquals(newDetailedStatus, savedStack.getStackStatus().getDetailedStackStatus());
            assertEquals(TRANSFORMED_STATUS_REASON, savedStack.getStackStatus().getStatusReason());

            // Verify InMemoryStateStore update logic
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.putStack(eq(STACK_ID), eq(PollGroup.POLLABLE)), times(1));
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.deleteStack(anyLong()), never());

            assertEquals(updatedStack, savedStack);
        }
    }

    @Test
    void testUpdateStackStatusByIdNoStatusChangeInMemoryStoreMissing() {
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.UPDATE_IN_PROGRESS;
        Status newStatus = newDetailedStatus.getStatus();
        initialStackStatus = new StackStatus(stack, newStatus, INITIAL_STATUS_REASON, newDetailedStatus);
        stack.setStackStatus(initialStackStatus);

        // Use mockStatic for InMemoryStateStore as it has static methods
        try (MockedStatic<InMemoryStateStore> mockedInMemoryStateStore = mockStatic(InMemoryStateStore.class)) {
            mockedInMemoryStateStore.when(() -> InMemoryStateStore.getStack(STACK_ID)).thenReturn(null);
            when(serviceStatusRawMessageTransformer.transformMessage(anyString(), any())).thenReturn(INITIAL_STATUS_REASON);
            when(stackStatusMessageTransformator.transformMessage(anyString())).thenReturn(INITIAL_STATUS_REASON);

            Stack updatedStack = underTest.updateStackStatus(STACK_ID, newDetailedStatus, INITIAL_STATUS_REASON);

            // Verify stackService.getStackById was called
            verify(stackService, times(1)).getStackById(STACK_ID);
            // Verify message transformation (still happens to check if reason is truly identical after transformation)
            verify(serviceStatusRawMessageTransformer, times(1)).transformMessage(eq(INITIAL_STATUS_REASON), any());
            verify(stackStatusMessageTransformator, times(1)).transformMessage(eq(INITIAL_STATUS_REASON));

            // Verify that save was NOT called
            verify(stackService, never()).save(any(Stack.class));

            // Verify InMemoryStateStore update logic (should be called because it was missing)
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.putStack(eq(STACK_ID), eq(PollGroup.POLLABLE)), times(1));
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.deleteStack(anyLong()), never());

            // Assert that the stack object itself wasn't changed (status remains initial)
            assertEquals(initialStackStatus.getStatus(), updatedStack.getStackStatus().getStatus());
            assertEquals(initialStackStatus.getDetailedStackStatus(), updatedStack.getStackStatus().getDetailedStackStatus());
            assertEquals(initialStackStatus.getStatusReason(), updatedStack.getStackStatus().getStatusReason());
        }
    }

    @Test
    void testUpdateStackStatusByIdNoStatusChangeInMemoryStorePresent() {
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.AVAILABLE;

        // Use mockStatic for InMemoryStateStore as it has static methods
        try (MockedStatic<InMemoryStateStore> mockedInMemoryStateStore = mockStatic(InMemoryStateStore.class)) {
            mockedInMemoryStateStore.when(() -> InMemoryStateStore.getStack(STACK_ID)).thenReturn(PollGroup.POLLABLE);
            when(serviceStatusRawMessageTransformer.transformMessage(anyString(), any())).thenReturn(INITIAL_STATUS_REASON);
            when(stackStatusMessageTransformator.transformMessage(anyString())).thenReturn(INITIAL_STATUS_REASON);

            Stack updatedStack = underTest.updateStackStatus(STACK_ID, newDetailedStatus, INITIAL_STATUS_REASON);

            // Verify stackService.getStackById was called
            verify(stackService, times(1)).getStackById(STACK_ID);
            // Verify message transformation
            verify(serviceStatusRawMessageTransformer, times(1)).transformMessage(eq(INITIAL_STATUS_REASON), any());
            verify(stackStatusMessageTransformator, times(1)).transformMessage(eq(INITIAL_STATUS_REASON));

            // Verify that save was NOT called
            verify(stackService, never()).save(any(Stack.class));

            // Verify InMemoryStateStore update logic (should NOT be called)
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.putStack(anyLong(), any()), never());
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.deleteStack(anyLong()), never());

            // Assert that the stack object itself wasn't changed (status remains initial)
            assertEquals(initialStackStatus.getStatus(), updatedStack.getStackStatus().getStatus());
            assertEquals(initialStackStatus.getDetailedStackStatus(), updatedStack.getStackStatus().getDetailedStackStatus());
            assertEquals(initialStackStatus.getStatusReason(), updatedStack.getStackStatus().getStatusReason());
        }
    }

    @Test
    void testUpdateStackStatusByIdDeleteCompletedExistingStatus() {
        stack.getStackStatus().setStatus(Status.DELETE_COMPLETED);
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.DELETE_IN_PROGRESS;
        String newReason = "deleting";

        try (MockedStatic<InMemoryStateStore> mockedInMemoryStateStore = mockStatic(InMemoryStateStore.class)) {
            mockedInMemoryStateStore.when(() -> InMemoryStateStore.getStack(STACK_ID)).thenReturn(null);

            Stack updatedStack = underTest.updateStackStatus(STACK_ID, newDetailedStatus, newReason);

            // Verify no calls to transform messages or save
            verify(serviceStatusRawMessageTransformer, never()).transformMessage(anyString(), any());
            verify(stackStatusMessageTransformator, never()).transformMessage(anyString());
            verify(stackService, never()).save(any(Stack.class));

            // Verify InMemoryStateStore was not touched by this path, as it's already DELETE_COMPLETED
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.putStack(anyLong(), any()), never());
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.deleteStack(anyLong()), never());

            // Assert that the stack object itself wasn't changed
            assertEquals(Status.DELETE_COMPLETED, updatedStack.getStackStatus().getStatus());
            assertEquals(DetailedStackStatus.AVAILABLE, updatedStack.getStackStatus().getDetailedStackStatus());
            assertEquals(INITIAL_STATUS_REASON, updatedStack.getStackStatus().getStatusReason());
        }
    }

    @Test
    void testUpdateStackStatusByIdNewStatusRemovable() {
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.STOPPED;
        Status newStatus = newDetailedStatus.getStatus();

        try (MockedStatic<InMemoryStateStore> mockedInMemoryStateStore = mockStatic(InMemoryStateStore.class)) {
            mockedInMemoryStateStore.when(() -> InMemoryStateStore.getStack(STACK_ID)).thenReturn(PollGroup.POLLABLE);

            Stack updatedStack = underTest.updateStackStatus(STACK_ID, newDetailedStatus, NEW_STATUS_REASON);

            verify(stackService, times(1)).getStackById(STACK_ID);
            verify(stackService, times(1)).save(any(Stack.class));

            // Verify InMemoryStateStore.deleteStack is called
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.deleteStack(eq(STACK_ID)), times(1));
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.putStack(anyLong(), any()), never());

            assertEquals(newStatus, updatedStack.getStackStatus().getStatus());
            assertEquals(newDetailedStatus, updatedStack.getStackStatus().getDetailedStackStatus());
        }
    }

    @Test
    void testUpdateStackStatusByStackObjectStatusChanged() {
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.PROVISION_REQUESTED;
        Status newStatus = newDetailedStatus.getStatus();

        try (MockedStatic<InMemoryStateStore> mockedInMemoryStateStore = mockStatic(InMemoryStateStore.class)) {
            mockedInMemoryStateStore.when(() -> InMemoryStateStore.getStack(STACK_ID)).thenReturn(PollGroup.POLLABLE);

            Stack updatedStack = underTest.updateStackStatus(stack, newDetailedStatus, NEW_STATUS_REASON);

            // Verify stackService.getStackById was NOT called in this path
            verify(stackService, never()).getStackById(anyLong());
            // Verify message transformation
            verify(serviceStatusRawMessageTransformer, times(1)).transformMessage(eq(NEW_STATUS_REASON), any());
            verify(stackStatusMessageTransformator, times(1)).transformMessage(eq(RAW_NEW_STATUS_REASON));

            // Verify saveStackNewStatus logic
            ArgumentCaptor<Stack> stackCaptor = ArgumentCaptor.forClass(Stack.class);
            verify(stackService, times(1)).save(stackCaptor.capture());

            Stack savedStack = stackCaptor.getValue();
            assertNotNull(savedStack.getStackStatus());
            assertEquals(newStatus, savedStack.getStackStatus().getStatus());
            assertEquals(newDetailedStatus, savedStack.getStackStatus().getDetailedStackStatus());
            assertEquals(TRANSFORMED_STATUS_REASON, savedStack.getStackStatus().getStatusReason());

            // Verify InMemoryStateStore update logic
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.putStack(eq(STACK_ID), eq(PollGroup.POLLABLE)), times(1));
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.deleteStack(anyLong()), never());

            assertEquals(updatedStack, savedStack);
        }
    }

    @Test
    void testUpdateStackStatusByStackObjectNoStatusChange() {
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.AVAILABLE;
        String newReason = INITIAL_STATUS_REASON;

        try (MockedStatic<InMemoryStateStore> mockedInMemoryStateStore = mockStatic(InMemoryStateStore.class)) {
            mockedInMemoryStateStore.when(() -> InMemoryStateStore.getStack(STACK_ID)).thenReturn(PollGroup.POLLABLE);
            when(serviceStatusRawMessageTransformer.transformMessage(anyString(), any())).thenReturn(INITIAL_STATUS_REASON);
            when(stackStatusMessageTransformator.transformMessage(anyString())).thenReturn(INITIAL_STATUS_REASON);

            Stack updatedStack = underTest.updateStackStatus(stack, newDetailedStatus, newReason);

            // Verify stackService.getStackById was NOT called
            verify(stackService, never()).getStackById(anyLong());
            // Verify message transformation
            verify(serviceStatusRawMessageTransformer, times(1)).transformMessage(eq(newReason), any());
            verify(stackStatusMessageTransformator, times(1)).transformMessage(eq(INITIAL_STATUS_REASON));

            // Verify that save was NOT called
            verify(stackService, never()).save(any(Stack.class));

            // Verify InMemoryStateStore update logic (should NOT be called)
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.putStack(anyLong(), any()), never());
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.deleteStack(anyLong()), never());

            // Assert that the stack object itself wasn't changed
            assertEquals(initialStackStatus.getStatus(), updatedStack.getStackStatus().getStatus());
            assertEquals(initialStackStatus.getDetailedStackStatus(), updatedStack.getStackStatus().getDetailedStackStatus());
            assertEquals(initialStackStatus.getStatusReason(), updatedStack.getStackStatus().getStatusReason());
        }
    }

    @Test
    void testUpdateStackStatusByStackObjectOptimisticLockingFailureException() {
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.UPDATE_IN_PROGRESS;
        Status newStatus = newDetailedStatus.getStatus();

        when(stackService.save(any(Stack.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException("Simulated optimistic locking failure", null))
                .thenReturn(stack);

        Stack refetchedStack = new Stack();
        refetchedStack.setId(STACK_ID);
        refetchedStack.setResourceCrn(STACK_CRN);
        StackStatus refetchedStackStatus = new StackStatus(refetchedStack, Status.AVAILABLE, INITIAL_STATUS_REASON, DetailedStackStatus.AVAILABLE);
        refetchedStack.setStackStatus(refetchedStackStatus);
        when(stackService.getStackById(STACK_ID)).thenReturn(refetchedStack);


        try (MockedStatic<InMemoryStateStore> mockedInMemoryStateStore = mockStatic(InMemoryStateStore.class)) {
            mockedInMemoryStateStore.when(() -> InMemoryStateStore.getStack(STACK_ID)).thenReturn(PollGroup.POLLABLE);

            Stack updatedStack = underTest.updateStackStatus(stack, newDetailedStatus, NEW_STATUS_REASON);

            // Verify save was called twice (once failed, once successful after retry)
            verify(stackService, times(2)).save(any(Stack.class));
            // Verify getStackById was called once (for the retry)
            verify(stackService, times(1)).getStackById(STACK_ID);

            // Verify message transformation (called twice, once for initial, once for retry)
            verify(serviceStatusRawMessageTransformer, times(2)).transformMessage(eq(NEW_STATUS_REASON), any());
            verify(stackStatusMessageTransformator, times(2)).transformMessage(eq(RAW_NEW_STATUS_REASON));


            // Verify InMemoryStateStore update logic (called twice, once for initial attempt's setup, once for retry)
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.putStack(eq(STACK_ID), eq(PollGroup.POLLABLE)), times(1));
            mockedInMemoryStateStore.verify(() -> InMemoryStateStore.deleteStack(anyLong()), never());

            // Assert that the final returned stack reflects the updated status
            assertEquals(newStatus, updatedStack.getStackStatus().getStatus());
            assertEquals(newDetailedStatus, updatedStack.getStackStatus().getDetailedStackStatus());
            assertEquals(TRANSFORMED_STATUS_REASON, updatedStack.getStackStatus().getStatusReason());
        }
    }

}
