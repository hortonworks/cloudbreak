package com.sequenceiq.cloudbreak.job.instancechecker.scheduler;

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

import com.sequenceiq.cloudbreak.quartz.metric.JobMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.SchedulerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.metric.TriggerMetricsListener;
import com.sequenceiq.cloudbreak.quartz.statuschecker.ResourceCheckerJobListener;

@ExtendWith(MockitoExtension.class)
class InstanceCheckerSchedulerFactoryConfigTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ObjectProvider objectProvider;

    @Mock
    private DataSource dataSource;

    @Spy
    private ResourceCheckerJobListener resourceCheckerJobListener;

    @InjectMocks
    private InstanceCheckerSchedulerFactoryConfig underTest;

    @Test
    void testMeteringSchedulerShouldHaveProperConfiguration() throws Exception {
        SchedulerFactoryBean meteringScheduler = underTest.quartzMeteringScheduler(new QuartzProperties(), objectProvider, applicationContext, dataSource);
        meteringScheduler.afterPropertiesSet();
        Scheduler scheduler = meteringScheduler.getScheduler();
        assertEquals("quartzInstanceCheckerScheduler", scheduler.getSchedulerName());
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