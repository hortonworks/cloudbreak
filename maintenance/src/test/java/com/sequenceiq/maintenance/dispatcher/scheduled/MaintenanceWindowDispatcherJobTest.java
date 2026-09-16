package com.sequenceiq.maintenance.dispatcher.scheduled;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.maintenance.dispatcher.MaintenanceWindowDispatchTickService;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowDispatcherJobTest {

    @Mock
    private MaintenanceWindowDispatchTickService dispatchTickService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private MaintenanceWindowDispatcherJob underTest;

    @Test
    void executeTracedJobInvokesDispatchTick() throws JobExecutionException {
        underTest.executeTracedJob(jobExecutionContext);

        verify(dispatchTickService).tick();
    }

    @Test
    void executeTracedJobPropagatesRuntimeFailure() {
        doThrow(new RuntimeException("tick failed")).when(dispatchTickService).tick();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(jobExecutionContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("tick failed");
    }
}
