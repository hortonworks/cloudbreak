package com.sequenceiq.freeipa.sync.dynamicentitlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;

@ExtendWith(MockitoExtension.class)
class DynamicEntitlementRefreshJobServiceTest {

    private static final String LOCAL_ID = "1";

    private static final int ERROR_COUNT = 5;

    private static final int ORIGINAL_INTERVAL = 10;

    @Mock
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @Mock
    private TransactionalScheduler scheduler;

    @Mock
    private Clock clock;

    @Mock
    private JobKey jobKey;

    @InjectMocks
    private DynamicEntitlementRefreshJobService underTest;

    @Test
    void testSchedule() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(dynamicEntitlementRefreshConfig.getIntervalInMinutes()).thenReturn(10);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        when(clock.getCurrentDateLowPrecision()).thenCallRealMethod();
        underTest.schedule(new DynamicEntitlementRefreshJobAdapter(jobResource));
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testUnscheduleIfScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        JobDetail jobDetail = mock(JobDetail.class);
        when(scheduler.getJobDetail(eq(jobKey))).thenReturn(jobDetail);
        underTest.unschedule(jobKey);
        verify(scheduler, times(1)).deleteJob(eq(jobKey));
    }

    @Test
    void testUnscheduleIfNotScheduled() throws TransactionService.TransactionExecutionException {
        underTest.unschedule(eq(jobKey));
        verify(scheduler, never()).deleteJob(eq(jobKey));
    }

    @Test
    void testRescheduleWithBackoff() throws TransactionService.TransactionExecutionException {
        JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getKey()).thenReturn(jobKey);
        when(jobKey.getName()).thenReturn(LOCAL_ID);
        when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap());
        when(dynamicEntitlementRefreshConfig.getIntervalInMinutes()).thenReturn(ORIGINAL_INTERVAL);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        Date now = new Date();
        when(clock.getCurrentDateLowPrecision()).thenReturn(now);

        underTest.reScheduleWithBackoff(1L, jobDetail, ERROR_COUNT);

        ArgumentCaptor<Trigger> triggerArgumentCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(eq(jobDetail), triggerArgumentCaptor.capture());
        long diff = ChronoUnit.MINUTES.between(now.toInstant(), triggerArgumentCaptor.getValue().getStartTime().toInstant().atZone(ZoneId.systemDefault()));
        assertEquals((2 << ERROR_COUNT) + ORIGINAL_INTERVAL, diff);
    }

}