package com.sequenceiq.periscope.monitor;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.periscope.domain.PeriscopeJob;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.monitor.handler.ClusterDeleteHandler;
import com.sequenceiq.periscope.repository.PeriscopeJobRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;

@ExtendWith(MockitoExtension.class)
public class DeleteMonitorTest {

    private static final String JOB_NAME = "delete-monitor";

    @Mock
    private PeriscopeJobRepository periscopeJobRepository;

    @Mock
    private ClusterDeleteHandler clusterDeleteHandler;

    @InjectMocks
    private DeleteMonitor deleteMonitor;

    @Test
    void testExecuteWithTsAsDefault() {
        deleteMonitor.execute(getContext());
        verify(clusterDeleteHandler).deleteClusters(anyLong());
    }

    @Test
    void testExecuteWithTsAsLastExecutedTime() {
        Long lastExecuted = 100L;
        PeriscopeJob periscopeJob = mock(PeriscopeJob.class);
        when(periscopeJob.getLastExecuted()).thenReturn(lastExecuted);
        when(periscopeJobRepository.findById(JOB_NAME)).thenReturn(Optional.of(periscopeJob));
        deleteMonitor.execute(getContext());
        verify(clusterDeleteHandler).deleteClusters(lastExecuted);
        verify(periscopeJob).setLastExecuted(anyLong());
    }

    private JobExecutionContext getContext() {
        JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap jobDataMap = mock(JobDataMap.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(jobDataMap.get("APPLICATION_CONTEXT")).thenReturn(applicationContext);
        when(applicationContext.getBean(ExecutorServiceWithRegistry.class)).thenReturn(mock(ExecutorServiceWithRegistry.class));
        when(applicationContext.getBean(RejectedThreadService.class)).thenReturn(mock(RejectedThreadService.class));
        when(applicationContext.getBean(ClusterService.class)).thenReturn(mock(ClusterService.class));
        when(applicationContext.getBean(PeriscopeNodeConfig.class)).thenReturn(mock(PeriscopeNodeConfig.class));
        when(applicationContext.getBean(ClusterDeleteHandler.class)).thenReturn(clusterDeleteHandler);
        when(applicationContext.getBean(PeriscopeJobRepository.class)).thenReturn(periscopeJobRepository);
        return jobExecutionContext;
    }
}
