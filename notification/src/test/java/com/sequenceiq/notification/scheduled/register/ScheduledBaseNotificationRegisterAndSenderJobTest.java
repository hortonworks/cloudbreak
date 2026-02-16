package com.sequenceiq.notification.scheduled.register;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.domain.Subscription;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.service.NotificationSendingService;

@ExtendWith(MockitoExtension.class)
class ScheduledBaseNotificationRegisterAndSenderJobTest {

    private static final String JOB_NAME = "test-job";

    private static final String RESOURCE_CRN = "crn:cdp:test:us-west-1:tenant:resource:123";

    @Mock
    private TransactionalScheduler scheduler;

    @Mock
    private NotificationSendingService notificationSendingService;

    private TestScheduledJob underTest;

    @BeforeEach
    void setUp() {
        underTest = new TestScheduledJob(scheduler, notificationSendingService);
    }

    @Test
    void initJobsSchedulesWhenEnabled() throws Exception {
        underTest.setEnabled(true);
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(null);
        doNothing().when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

        underTest.initJobs();

        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void initJobsDoesNotScheduleWhenDisabled() throws Exception {
        underTest.setEnabled(false);

        underTest.initJobs();

        verify(scheduler, times(0)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void executeTracedJobProcessesNotifications() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        NotificationGeneratorDto dto = NotificationGeneratorDto.builder()
                .resourceCrn(RESOURCE_CRN)
                .name("test")
                .build();
        underTest.setData(List.of(dto));

        underTest.executeTracedJob(context);

        verify(notificationSendingService).processAndImmediatelySend(any(NotificationGeneratorDtos.class));
    }

    @Test
    void executeTracedJobHandlesExceptionGracefully() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        underTest.setDataToThrowException(true);

        underTest.executeTracedJob(context);

        verify(notificationSendingService, times(0)).processAndImmediatelySend(any());
    }

    @Test
    void getJobGroupReturnsCorrectValue() {
        String jobGroup = underTest.getJobGroup();

        assertNotNull(jobGroup);
        assertTrue(jobGroup.contains(JOB_NAME));
        assertTrue(jobGroup.contains("notification-register-job-group"));
    }

    @Test
    void getSchedulerReturnsScheduler() {
        assertEquals(scheduler, underTest.getScheduler());
    }

    @Test
    void onSubscriptionsProcessedDefaultImplementation() {
        List<Subscription> subscriptions = List.of();

        underTest.onSubscriptionsProcessed(subscriptions);

        verify(notificationSendingService, times(0)).processAndImmediatelySend(any());
    }

    @Test
    void scheduleCreatesJobAndTrigger() throws Exception {
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(null);
        doNothing().when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

        underTest.schedule();

        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void scheduleUnschedulesExistingJobFirst() throws Exception {
        JobDetail existingJob = mock(JobDetail.class);
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(existingJob);
        doNothing().when(scheduler).deleteJob(any(JobKey.class));
        doNothing().when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

        underTest.schedule();

        verify(scheduler).deleteJob(any(JobKey.class));
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void unscheduleDeletesJob() throws Exception {
        doNothing().when(scheduler).deleteJob(any(JobKey.class));

        underTest.unschedule();

        verify(scheduler).deleteJob(any(JobKey.class));
    }

    @Test
    void executeTracedJobBuildsCorrectNotificationDtos() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        NotificationGeneratorDto dto = NotificationGeneratorDto.builder()
                .resourceCrn(RESOURCE_CRN)
                .name("test-resource")
                .build();
        underTest.setData(List.of(dto));

        underTest.executeTracedJob(context);

        verify(notificationSendingService).processAndImmediatelySend(any(NotificationGeneratorDtos.class));
    }

    @Test
    void cronScheduleIsUsedWhenProvided() throws Exception {
        underTest.setCron("0 */5 * * * ?");
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(null);
        doNothing().when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

        underTest.schedule();

        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void intervalInHoursIsUsedWhenProvided() throws Exception {
        underTest.setIntervalInHours(2);
        underTest.setCron(null);
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(null);
        doNothing().when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

        underTest.schedule();

        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    static class TestScheduledJob extends AbstractScheduledNotificationJob {

        private boolean enabled = true;

        private Collection<NotificationGeneratorDto> data = List.of();

        private boolean dataToThrowException;

        private String cron = "0 */3 * * * ?";

        private Integer intervalInHours;

        TestScheduledJob(TransactionalScheduler scheduler, NotificationSendingService notificationSendingService) {
            super(scheduler, notificationSendingService);
        }

        @Override
        protected String getName() {
            return JOB_NAME;
        }

        @Override
        protected boolean enabled() {
            return enabled;
        }

        @Override
        protected Collection<NotificationGeneratorDto> data() {
            if (dataToThrowException) {
                throw new RuntimeException("Test exception");
            }
            return data;
        }

        @Override
        protected NotificationType notificationType() {
            return NotificationType.AZURE_DEFAULT_OUTBOUND;
        }

        @Override
        protected Optional<Integer> intervalInHours() {
            return Optional.ofNullable(intervalInHours);
        }

        @Override
        protected Optional<String> cron() {
            return Optional.ofNullable(cron);
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setData(Collection<NotificationGeneratorDto> data) {
            this.data = data;
        }

        public void setDataToThrowException(boolean dataToThrowException) {
            this.dataToThrowException = dataToThrowException;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public void setIntervalInHours(Integer intervalInHours) {
            this.intervalInHours = intervalInHours;
        }
    }
}