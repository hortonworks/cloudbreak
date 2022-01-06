package com.sequenceiq.cloudbreak.core.flow2;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;

@ExtendWith(MockitoExtension.class)
public class StackStatusFinalizerTest {

    private static final Long RESOURCE_ID = 1L;

    @Mock
    private StackStatusService stackStatusService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private StackStatusFinalizer underTest;

    @Test
    public void shouldUpdateWhenIsInProgressStatus() {
        when(stackStatusService.findFirstByStackIdOrderByCreatedDesc(RESOURCE_ID))
                .thenReturn(Optional.of(new StackStatus<>(new Stack(), Status.UPDATE_IN_PROGRESS, "", DetailedStackStatus.UNKNOWN)));

        underTest.onFinalize(RESOURCE_ID);

        verify(stackUpdater).updateStackStatusAndSetDetailedStatusToUnknown(RESOURCE_ID, Status.UPDATE_FAILED);
    }

    @Test
    public void shouldNotUpdateWhenIsInFinalStatus() {
        when(stackStatusService.findFirstByStackIdOrderByCreatedDesc(RESOURCE_ID))
                .thenReturn(Optional.of(new StackStatus<>(new Stack(), Status.AVAILABLE, "", DetailedStackStatus.UNKNOWN)));

        underTest.onFinalize(RESOURCE_ID);

        verifyNoInteractions(stackUpdater);
    }
}