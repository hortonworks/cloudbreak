package com.sequenceiq.cloudbreak.quartz.statuschecker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@ExtendWith(MockitoExtension.class)
class StatusCheckerJobServiceTest {

    private static final int LARGE_INTERVAL_SECONDS = 7200000;

    private static final int SNOOZE_SECONDS = 100;

    private static final int FIRST_DELAY = 100;

    private static final int MID_DELAY = 3600000;

    private static final int LAST_DELAY = 7199999;

    private static final int EXPECTED_SCHEDULE_COUNT = 3;

    private static final long TIMING_TOLERANCE_MILLIS = 5000L;

    private static final long MILLIS_MULTIPLIER = 1000L;

    @Mock
    private StatusCheckerConfig statusCheckerConfig;

    @Mock private TransactionalScheduler scheduler;

    @Mock private ApplicationContext applicationContext;

    @InjectMocks private StatusCheckerJobService statusCheckerJobService;

    @Mock private JobResourceAdapter<Object> resourceAdapter;

    @Mock private JobResource jobResource;

    @Test
    void testScheduleCreatesUniformlyDistributedTriggers() throws Exception {
        when(statusCheckerConfig.getIntervalInSeconds()).thenReturn(LARGE_INTERVAL_SECONDS);
        when(statusCheckerConfig.getSnoozeSeconds()).thenReturn(SNOOZE_SECONDS);
        mockResourceAdapter();

        try (MockedStatic<RandomUtil> randomMock = mockStatic(RandomUtil.class)) {
            int[] delays = {FIRST_DELAY, MID_DELAY, LAST_DELAY};
            AtomicInteger callCount = new AtomicInteger(0);
            randomMock.when(() -> RandomUtil.getQuickRandomInt(LARGE_INTERVAL_SECONDS)).thenAnswer(inv -> delays[callCount.getAndIncrement()]);

            ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
            for (int i = 0; i < delays.length; i++) {
                statusCheckerJobService.schedule(resourceAdapter);
}
            verify(scheduler, times(EXPECTED_SCHEDULE_COUNT)).scheduleJob(any(JobDetail.class), captor.capture());
            validateTriggers(captor.getAllValues(), delays, SNOOZE_SECONDS, LARGE_INTERVAL_SECONDS);
        }
    }

    @Test
    void testScheduleLongIntervalCheckCreatesUniformlyDistributedTriggers() throws Exception {
        when(statusCheckerConfig.getLongIntervalInSeconds()).thenReturn(LARGE_INTERVAL_SECONDS);
        mockResourceAdapter();

        try (MockedStatic<RandomUtil> randomMock = mockStatic(RandomUtil.class)) {
            randomMock.when(() -> RandomUtil.getQuickRandomInt(LARGE_INTERVAL_SECONDS)).thenReturn(MID_DELAY);

            ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
            statusCheckerJobService.scheduleLongIntervalCheck(resourceAdapter);

            verify(scheduler).scheduleJob(any(JobDetail.class), captor.capture());
            SimpleTrigger trigger = (SimpleTrigger) captor.getValue();
            assertThat(trigger.getRepeatInterval()).isEqualTo(LARGE_INTERVAL_SECONDS * MILLIS_MULTIPLIER);
        }
    }

    private void mockResourceAdapter() {
        when(resourceAdapter.getJobResource()).thenReturn(jobResource);
        when(resourceAdapter.getJobClassForResource()).thenReturn((Class) TestJob.class);
        when(resourceAdapter.toJobDataMap()).thenReturn(new JobDataMap());
        when(jobResource.getLocalId()).thenReturn("test-id");
    }

    private void validateTriggers(List<Trigger> triggers, int[] delays, int snooze, int interval) {
        for (int i = 0; i < triggers.size(); i++) {
            SimpleTrigger trigger = (SimpleTrigger) triggers.get(i);
            long expectedDelay = (snooze + delays[i]) * MILLIS_MULTIPLIER;
            long actualDelay = trigger.getStartTime().getTime() - System.currentTimeMillis();
            assertThat(actualDelay).isBetween(expectedDelay - TIMING_TOLERANCE_MILLIS, expectedDelay + TIMING_TOLERANCE_MILLIS);
            assertThat(trigger.getRepeatInterval()).isEqualTo(interval * MILLIS_MULTIPLIER);
        }
    }

    private static class TestJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            // nothing to do here
        }
    }
}
