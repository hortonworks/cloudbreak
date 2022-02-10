package com.sequenceiq.redbeams.sync;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@ExtendWith(MockitoExtension.class)
public class DBStackJobServiceTest {

    private static final Long DB_STACK_ID = 123L;

    @Mock
    private AutoSyncConfig autoSyncConfig;

    @Mock
    private StatusCheckerJobService jobService;

    @Mock
    private DBStack dbStack;

    @InjectMocks
    private DBStackJobService victim;

    @Test
    public void shouldScheduleJobWhenEnabled() {
        when(autoSyncConfig.isEnabled()).thenReturn(true);

        victim.schedule(DB_STACK_ID);

        verify(jobService).schedule(DB_STACK_ID, DBStackJobAdapter.class);
    }

    @Test
    public void shouldNotScheduleJobWhenDisabled() {
        when(autoSyncConfig.isEnabled()).thenReturn(false);

        victim.schedule(DB_STACK_ID);

        verifyNoInteractions(jobService);
    }

    @Test
    public void shouldUnscheduleJob() {
        victim.unschedule(DB_STACK_ID, "name");

        verify(jobService).unschedule(String.valueOf(DB_STACK_ID));
    }
}