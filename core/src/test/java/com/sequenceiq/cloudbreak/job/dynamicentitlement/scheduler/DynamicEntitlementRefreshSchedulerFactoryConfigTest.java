package com.sequenceiq.cloudbreak.job.dynamicentitlement.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.quartz.metric.JobMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.SchedulerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.TriggerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.ResourceCheckerJobListener;

import io.micrometer.core.instrument.MeterRegistry;

@ExtendWith(MockitoExtension.class)
class DynamicEntitlementRefreshSchedulerFactoryConfigTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ObjectProvider objectProvider;

    @Mock
    private DataSource dataSource;

    @Mock
    private MetricService metricService;

    @Spy
    private ResourceCheckerJobListener resourceCheckerJobListener;

    @InjectMocks
    private DynamicEntitlementRefreshSchedulerFactoryConfig underTest;

    @Test
    void testDynamicEntitlementSchedulerShouldHaveProperConfiguration() throws Exception {
        SchedulerFactoryBean meteringScheduler = underTest.quartzDynamicEntitlementRefreshScheduler(new QuartzProperties(), objectProvider, applicationContext,
                dataSource);
        meteringScheduler.afterPropertiesSet();
        Scheduler scheduler = meteringScheduler.getScheduler();
        assertEquals("quartzDynamicEntitlementRefreshScheduler", scheduler.getSchedulerName());
        ListenerManager listenerManager = scheduler.getListenerManager();
        assertThat(listenerManager.getSchedulerListeners()).hasSize(1);
        assertEquals(SchedulerMetricsListener.class, listenerManager.getSchedulerListeners().getFirst().getClass());
        assertThat(listenerManager.getJobListeners()).hasSize(2);
        assertNotNull(listenerManager.getJobListener(ResourceCheckerJobListener.class.getSimpleName()));
        assertNotNull(listenerManager.getJobListener(JobMetricsListener.class.getSimpleName()));
        assertThat(listenerManager.getTriggerListeners()).hasSize(1);
        assertNotNull(listenerManager.getTriggerListener(TriggerMetricsListener.class.getSimpleName()));
    }

}