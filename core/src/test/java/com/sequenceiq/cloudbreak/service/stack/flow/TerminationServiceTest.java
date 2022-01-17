package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class TerminationServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private FreeIpaCleanupService freeIpaCleanupService;

    @InjectMocks
    private TerminationService underTest;

    @Test
    void testFinalizeRecoveryFroInstancesWithoutInstanceIds() {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);
        InstanceMetaData im3 = mock(InstanceMetaData.class);
        InstanceMetaData im4 = mock(InstanceMetaData.class);

        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getNotTerminatedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2, im3, im4));

        underTest.finalizeRecoveryTeardown(1L);

        verify(freeIpaCleanupService).cleanupButIp(stack);
        verify(instanceMetaDataService).delete(im1);
        verify(instanceMetaDataService).delete(im2);
        verify(instanceMetaDataService).delete(im3);
        verify(instanceMetaDataService).delete(im4);
        verifyNoInteractions(instanceGroupService);
    }

}