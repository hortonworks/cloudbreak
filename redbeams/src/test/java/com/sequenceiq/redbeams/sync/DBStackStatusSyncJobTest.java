package com.sequenceiq.redbeams.sync;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

import io.opentracing.Tracer;

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

    @Mock
    private Tracer tracer;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

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

        victim.executeTracedJob(jobExecutionContext);

        verifyZeroInteractions(dbStackStatusSyncService);
    }

    @Test
    public void shouldCallSync() throws JobExecutionException {
        when(flowLogService.isOtherFlowRunning(DB_STACK_ID)).thenReturn(false);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        victim.executeTracedJob(jobExecutionContext);

        verify(dbStackStatusSyncService).sync(dbStack);
    }
}