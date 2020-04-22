package com.sequenceiq.redbeams.sync;

import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DBStackStatusSyncJobTest {

    private static final String JOB_LOCAL_ID = "1234";

    private static final long DB_STACK_ID = 1234L;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStackStatusSyncService dbStackStatusSyncService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private DBStack dbStack;

    @InjectMocks
    private DBStackStatusSyncJob victim;

    @BeforeEach
    public void initTests() {
        MockitoAnnotations.initMocks(this);
        victim.setLocalId(JOB_LOCAL_ID);

        when(dbStackService.getById(DB_STACK_ID)).thenReturn(dbStack);
    }

    @Test
    public void shouldNotCallSyncWhenOtherFlowIsRunning() throws JobExecutionException {
        when(flowLogService.isOtherFlowRunning(DB_STACK_ID)).thenReturn(true);

        victim.executeInternal(jobExecutionContext);

        verifyZeroInteractions(dbStackStatusSyncService);
    }

    @Test
    public void shouldCallSync() throws JobExecutionException {
        when(flowLogService.isOtherFlowRunning(DB_STACK_ID)).thenReturn(false);

        victim.executeInternal(jobExecutionContext);

        verify(dbStackStatusSyncService).sync(dbStack);
    }
}