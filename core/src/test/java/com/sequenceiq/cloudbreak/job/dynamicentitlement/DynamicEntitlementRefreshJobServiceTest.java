package com.sequenceiq.cloudbreak.job.dynamicentitlement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.job.dynamicentitlement.scheduler.DynamicEntitlementRefreshTransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;

@ExtendWith(MockitoExtension.class)
class DynamicEntitlementRefreshJobServiceTest {

    private static final String LOCAL_ID = "1";

    @Mock
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @Mock
    private DynamicEntitlementRefreshTransactionalScheduler scheduler;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private DynamicEntitlementRefreshJobService underTest;

    @Test
    void testSchedule() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(dynamicEntitlementRefreshConfig.getIntervalInMinutes()).thenReturn(10);
        when(dynamicEntitlementRefreshConfig.isDynamicEntitlementEnabled()).thenReturn(Boolean.TRUE);
        underTest.schedule(new DynamicEntitlementRefreshJobAdapter(jobResource));
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testUnscheduleIfScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        JobKey jobKey = mock(JobKey.class);
        JobDetail jobDetail = mock(JobDetail.class);
        when(scheduler.getJobDetail(eq(jobKey))).thenReturn(jobDetail);
        underTest.unschedule(jobKey);
        verify(scheduler, times(1)).deleteJob(eq(jobKey));
    }

    @Test
    void testUnscheduleIfNotScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        JobKey jobKey = mock(JobKey.class);
        when(scheduler.getJobDetail(eq(jobKey))).thenReturn(null);
        underTest.unschedule(eq(jobKey));
        verify(scheduler, never()).deleteJob(eq(jobKey));
    }

}