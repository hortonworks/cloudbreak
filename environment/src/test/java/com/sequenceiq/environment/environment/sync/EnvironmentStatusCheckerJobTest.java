package com.sequenceiq.environment.environment.sync;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SYNC_FINISHED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.AVAILABLE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.ENV_STOPPED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
public class EnvironmentStatusCheckerJobTest {

    private final EnvironmentService environmentService = Mockito.mock(EnvironmentService.class);

    private final FlowLogService flowLogService = Mockito.mock(FlowLogService.class);

    private final EnvironmentSyncService environmentSyncService = Mockito.mock(EnvironmentSyncService.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService = Mockito.mock(EnvironmentStatusUpdateService.class);

    private final EnvironmentJobService environmentJobService = Mockito.mock(EnvironmentJobService.class);

    private final AutoSyncConfig autoSyncConfig = Mockito.mock(AutoSyncConfig.class);

    private final EnvironmentStatusCheckerJob underTest = new EnvironmentStatusCheckerJob(environmentService, flowLogService, environmentSyncService,
            environmentStatusUpdateService, environmentJobService, autoSyncConfig);

    @Test
    void testSyncAnEnvSameStatus() {
        Environment environment = new Environment();
        environment.setId(1234L);
        environment.setStatus(AVAILABLE);

        when(environmentSyncService.getStatusByFreeipa(environment)).thenReturn(AVAILABLE);

        underTest.syncAnEnv(environment);

        verify(environmentStatusUpdateService, never()).updateEnvironmentStatusAndNotify(eq(environment), any(), any());
    }

    @Test
    void testSyncAnEnvDifferentStatusAndUpdateEnabled() {
        Environment environment = new Environment();
        environment.setId(1234L);
        environment.setStatus(ENV_STOPPED);

        when(environmentSyncService.getStatusByFreeipa(environment)).thenReturn(AVAILABLE);
        when(autoSyncConfig.isUpdateStatus()).thenReturn(true);

        underTest.syncAnEnv(environment);

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(environment, AVAILABLE, ENVIRONMENT_SYNC_FINISHED);
    }

    @Test
    void testSyncAnEnvDifferentStatusAndUpdateDisabled() {
        Environment environment = new Environment();
        environment.setId(1234L);
        environment.setStatus(ENV_STOPPED);

        when(environmentSyncService.getStatusByFreeipa(environment)).thenReturn(AVAILABLE);
        when(autoSyncConfig.isUpdateStatus()).thenReturn(false);

        underTest.syncAnEnv(environment);

        verify(autoSyncConfig).isUpdateStatus();
        verify(environmentStatusUpdateService, never()).updateEnvironmentStatusAndNotify(environment, AVAILABLE, ENVIRONMENT_SYNC_FINISHED);
    }
}
