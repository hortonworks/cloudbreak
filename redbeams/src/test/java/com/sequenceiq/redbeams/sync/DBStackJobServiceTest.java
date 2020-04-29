package com.sequenceiq.redbeams.sync;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.statuschecker.service.JobService;
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
    private JobService jobService;

    @Mock
    private DBStack dbStack;

    @InjectMocks
    private DBStackJobService victim;

    @BeforeEach
    public void initTests() {
        initMocks(this);

        when(dbStack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
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
    public void shouldNotScheduleJobInCaseOfNonAws() {
        when(autoSyncConfig.isEnabled()).thenReturn(true);
        when(dbStack.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());

        victim.schedule(dbStack);

        verifyZeroInteractions(jobService);
    }

    @Test
    public void shouldUnscheduleJob() {
        when(dbStack.getId()).thenReturn(DB_STACK_ID);

        victim.unschedule(dbStack);

        verify(jobService).unschedule(String.valueOf(DB_STACK_ID));
    }

    @Test
    public void shouldNotUnscheduleJobInCaseOfNonAws() {
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());

        victim.unschedule(dbStack);

        verifyZeroInteractions(jobService);
    }
}