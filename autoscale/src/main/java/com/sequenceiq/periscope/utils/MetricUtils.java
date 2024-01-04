package com.sequenceiq.periscope.utils;

import static com.sequenceiq.periscope.domain.MetricType.THREADPOOL_ACTIVE_THREADS;
import static com.sequenceiq.periscope.domain.MetricType.THREADPOOL_QUEUE_SIZE;
import static com.sequenceiq.periscope.domain.MetricType.THREADPOOL_TASKS_COMPLETED;
import static com.sequenceiq.periscope.domain.MetricType.THREADPOOL_THREADS_TOTAL;

import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerScheduledExecutor;
import com.sequenceiq.periscope.service.PeriscopeMetricService;

@Service
public class MetricUtils {

    @Inject
    private PeriscopeMetricService metricService;

    public void submitThreadPoolExecutorParameters(ExecutorService executorService) {
        MDCCleanerScheduledExecutor threadPoolExecutor = (MDCCleanerScheduledExecutor) executorService;
        metricService.gauge(THREADPOOL_THREADS_TOTAL, threadPoolExecutor.getCorePoolSize());
        metricService.gauge(THREADPOOL_QUEUE_SIZE, threadPoolExecutor.getQueue().size());
        metricService.gauge(THREADPOOL_TASKS_COMPLETED, threadPoolExecutor.getCompletedTaskCount());
        metricService.gauge(THREADPOOL_ACTIVE_THREADS, threadPoolExecutor.getActiveCount());
    }
}
