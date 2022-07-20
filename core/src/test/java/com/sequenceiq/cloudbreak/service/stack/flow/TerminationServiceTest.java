package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class TerminationServiceTest {

    @Mock
    private StackDtoService stackDtoService;

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

}