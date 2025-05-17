package com.sequenceiq.cloudbreak.job.instancechecker.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;

@ExtendWith(MockitoExtension.class)
class InstanceCheckerTransactionalSchedulerTest {

    private static final String JOB_NAME = "JOB_NAME";

    @Mock
    private Scheduler scheduler;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private InstanceCheckerTransactionalScheduler underTest;

    @BeforeEach
    void setUp() throws TransactionService.TransactionExecutionException {
        doAnswer((Answer<Void>) invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
    }

    @Test
    void testClear() throws TransactionService.TransactionExecutionException, SchedulerException {
        underTest.clear();
        verify(scheduler, times(1)).clear();
    }

    @Test
    void scheduleJob() throws TransactionService.TransactionExecutionException, SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(MdcQuartzJob.class).build();
        Trigger trigger = TriggerBuilder.newTrigger().build();
        underTest.scheduleJob(jobDetail, trigger);
        verify(scheduler, times(1)).scheduleJob(eq(jobDetail), eq(trigger));
    }

    @Test
    void deleteJob() throws TransactionService.TransactionExecutionException, SchedulerException {
        JobKey jobKey = new JobKey(JOB_NAME);
        underTest.deleteJob(jobKey);
        verify(scheduler, times(1)).deleteJob(eq(jobKey));
    }
}