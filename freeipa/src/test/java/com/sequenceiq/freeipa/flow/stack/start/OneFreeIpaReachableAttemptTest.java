package com.sequenceiq.freeipa.flow.stack.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.FreeIpaInstanceHealthDetailsService;

@ExtendWith(MockitoExtension.class)
class OneFreeIpaReachableAttemptTest {

    public static final long ID = 99927L;

    @Mock
    private FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    private OneFreeIpaReachableAttempt oneFreeIpaReachableAttemptUnderTest;

    private Stack stack;

    private Set<InstanceMetaData> instanceSet;

    private RPCResponse<Boolean> response;

    @BeforeEach
    public void setup() {
        stack = new Stack();
        stack.setId(ID);
        InMemoryStateStore.putStack(ID, PollGroup.POLLABLE);

        InstanceMetaData instance1 = new InstanceMetaData();
        instance1.setInstanceId("ipaserver1");
        InstanceMetaData instance2 = new InstanceMetaData();
        instance2.setInstanceId("ipaserver2");
        instanceSet = Set.of(instance1, instance2);

        oneFreeIpaReachableAttemptUnderTest = new OneFreeIpaReachableAttempt(freeIpaInstanceHealthDetailsService, stack, instanceSet, 1);
        response = new RPCResponse<>();
    }

    @Test
    public void testAttemptSucceed() throws Exception {
        response.setResult(Boolean.TRUE);
        when(freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(any(), any())).thenReturn(response);
        assertEquals(AttemptState.FINISH, oneFreeIpaReachableAttemptUnderTest.process().getState());
        verify(freeIpaInstanceHealthDetailsService, times(1)).checkFreeIpaHealth(any(), any());
    }

    @Test
    public void testAttemptSecondTimeSucceed() throws Exception {
        response.setResult(Boolean.TRUE);
        oneFreeIpaReachableAttemptUnderTest = new OneFreeIpaReachableAttempt(freeIpaInstanceHealthDetailsService, stack, instanceSet, 2);
        when(freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(any(), any())).thenReturn(response);
        assertEquals(AttemptState.CONTINUE, oneFreeIpaReachableAttemptUnderTest.process().getState());
        assertEquals(AttemptState.FINISH, oneFreeIpaReachableAttemptUnderTest.process().getState());
        verify(freeIpaInstanceHealthDetailsService, times(3)).checkFreeIpaHealth(any(), any());
    }

    @Test
    public void testAttemptSecondTimeSucceedWithAFailure() throws Exception {
        response.setResult(Boolean.TRUE);
        oneFreeIpaReachableAttemptUnderTest = new OneFreeIpaReachableAttempt(freeIpaInstanceHealthDetailsService, stack, instanceSet, 2);
        when(freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(any(), any())).thenReturn(response);
        assertEquals(AttemptState.CONTINUE, oneFreeIpaReachableAttemptUnderTest.process().getState());
        response.setResult(Boolean.FALSE);
        assertEquals(AttemptState.CONTINUE, oneFreeIpaReachableAttemptUnderTest.process().getState());
        response.setResult(Boolean.TRUE);
        assertEquals(AttemptState.CONTINUE, oneFreeIpaReachableAttemptUnderTest.process().getState());
        assertEquals(AttemptState.FINISH, oneFreeIpaReachableAttemptUnderTest.process().getState());
        verify(freeIpaInstanceHealthDetailsService, times(7)).checkFreeIpaHealth(any(), any());
    }

    @Test
    public void testAttemptFailed() throws Exception {
        response.setResult(Boolean.FALSE);
        when(freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(any(), any())).thenReturn(response);
        assertEquals(oneFreeIpaReachableAttemptUnderTest.process().getState(), AttemptState.CONTINUE);
        verify(freeIpaInstanceHealthDetailsService, times(2)).checkFreeIpaHealth(any(), any());
    }

    @Test
    public void testAttemptFailedWithExcBaseNotificationRegisterAdditionalDataDtoeption() {
        response.setResult(Boolean.FALSE);
        when(freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(any(), any())).thenThrow(Retry.ActionFailedException.class);
        assertEquals(oneFreeIpaReachableAttemptUnderTest.process().getState(), AttemptState.CONTINUE);
        verify(freeIpaInstanceHealthDetailsService, times(2)).checkFreeIpaHealth(any(), any());
    }

    @Test
    public void tesStackPollGroupEmpty() throws Exception {
        InMemoryStateStore.deleteStack(ID);
        assertEquals(oneFreeIpaReachableAttemptUnderTest.process().getState(), AttemptState.BREAK);
    }

    @Test
    public void tesStackPollGroupCancelled() throws Exception {
        InMemoryStateStore.putStack(ID, PollGroup.CANCELLED);
        assertEquals(oneFreeIpaReachableAttemptUnderTest.process().getState(), AttemptState.BREAK);
    }
}