package com.sequenceiq.redbeams.sync;

import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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

    @BeforeEach
    public void initTests() {
        initMocks(this);
    }

    @Test
    public void shouldScheduleJobWhenEnabled() {
        when(autoSyncConfig.isEnabled()).thenReturn(true);

        victim.schedule(dbStack);

        ArgumentCaptor<DBStackJobAdapter> dbStackJobAdapterArgumentCaptor = ArgumentCaptor.forClass(DBStackJobAdapter.class);

        verify(jobService).schedule(dbStackJobAdapterArgumentCaptor.capture());

        assertEquals(dbStack, dbStackJobAdapterArgumentCaptor.getValue().getResource());
    }

    @Test
    public void shouldNotScheduleJobWhenDisabled() {
        when(autoSyncConfig.isEnabled()).thenReturn(false);

        victim.schedule(dbStack);

        verifyZeroInteractions(jobService);
    }

    @Test
    public void shouldUnscheduleJob() {
        when(dbStack.getId()).thenReturn(DB_STACK_ID);

        victim.unschedule(dbStack);

        verify(jobService).unschedule(String.valueOf(DB_STACK_ID));
    }
}