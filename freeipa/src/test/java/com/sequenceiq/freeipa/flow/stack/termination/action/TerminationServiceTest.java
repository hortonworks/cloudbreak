package com.sequenceiq.freeipa.flow.stack.termination.action;

import static org.mockito.Mockito.any;
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
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class TerminationServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private Clock clock;

    @InjectMocks
    private TerminationService underTest;

    @Test
    void testFinalizeTerminationFroInstancesWithoutInstanceIds() {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);
        InstanceMetaData im3 = mock(InstanceMetaData.class);
        InstanceMetaData im4 = mock(InstanceMetaData.class);

        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getAllInstanceMetaDataList()).thenReturn(List.of(im1, im2, im3, im4));
        when(im1.isTerminated()).thenReturn(true);
        when(im2.isTerminated()).thenReturn(false);
        when(im3.isTerminated()).thenReturn(false);
        when(im4.isTerminated()).thenReturn(false);
        when(im2.getInstanceId()).thenReturn(null);
        when(im3.getInstanceId()).thenReturn("i-3");
        when(im4.getInstanceId()).thenReturn(null);
        when(clock.getCurrentTimeMillis()).thenReturn(1L);

        underTest.finalizeTerminationForInstancesWithoutInstanceIds(1L);

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
}