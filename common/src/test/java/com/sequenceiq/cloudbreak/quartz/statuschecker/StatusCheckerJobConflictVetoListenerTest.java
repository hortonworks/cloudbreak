package com.sequenceiq.cloudbreak.quartz.statuschecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

@ExtendWith(MockitoExtension.class)
class StatusCheckerJobConflictVetoListenerTest {

    private static final String JOB_GROUP_NAME = "jobGroupName";

    private StatusCheckerJobConflictVetoListener underTest;

    @Mock
    private Trigger trigger;

    @Mock
    private JobExecutionContext context;

    @Mock
    private Scheduler scheduler;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private JobDetail jobDetail;

    @BeforeEach
    void setUp() throws SchedulerException {
        underTest = new StatusCheckerJobConflictVetoListener(JOB_GROUP_NAME);
        lenient().when(context.getScheduler()).thenReturn(scheduler);
        lenient().when(scheduler.getCurrentlyExecutingJobs()).thenReturn(List.of(jobExecutionContext));
        lenient().when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
    }

    @Test
    void differentGroup() {
        setTriggerJobKey("1", "other");
        setRunningJobKey("1", JOB_GROUP_NAME);

        boolean result = underTest.vetoJobExecution(trigger, context);

        assertThat(result).isFalse();
    }

    @Test
    void sameGroupButDifferentId() {
        setTriggerJobKey("1", JOB_GROUP_NAME);
        setRunningJobKey("2", JOB_GROUP_NAME);

        boolean result = underTest.vetoJobExecution(trigger, context);

        assertThat(result).isFalse();
    }

    @Test
    void sameGroupAndIdButDifferentSubgroup() {
        setTriggerJobKey("1-s1", JOB_GROUP_NAME);
        setRunningJobKey("1-s2", JOB_GROUP_NAME);

        boolean result = underTest.vetoJobExecution(trigger, context);

        assertThat(result).isTrue();
    }

    @Test
    void schedulerException() throws SchedulerException {
        setTriggerJobKey("1", JOB_GROUP_NAME);
        when(scheduler.getCurrentlyExecutingJobs()).thenThrow(SchedulerException.class);

        boolean result = underTest.vetoJobExecution(trigger, context);

        assertThat(result).isTrue();
    }

    private void setTriggerJobKey(String jobName, String jobGroupName) {
        lenient().when(trigger.getJobKey()).thenReturn(JobKey.jobKey(jobName, jobGroupName));
    }

    private void setRunningJobKey(String jobName, String jobGroupName) {
        lenient().when(jobDetail.getKey()).thenReturn(JobKey.jobKey(jobName, jobGroupName));
    }

}
