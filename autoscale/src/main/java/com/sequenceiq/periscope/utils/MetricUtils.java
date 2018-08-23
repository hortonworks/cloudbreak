package com.sequenceiq.periscope.utils;

import static com.sequenceiq.periscope.domain.MetricType.THREADPOOL_ACTIVE_THREADS;
import static com.sequenceiq.periscope.domain.MetricType.THREADPOOL_QUEUE_SIZE;
import static com.sequenceiq.periscope.domain.MetricType.THREADPOOL_TASKS_COMPLETED;
import static com.sequenceiq.periscope.domain.MetricType.THREADPOOL_THREADS_TOTAL;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.service.MetricService;

@Service
public class MetricUtils {

    @Inject
    private MetricService metricService;

    public void submitThreadPoolExecutorParameters(ExecutorService executorService) {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        metricService.submitGauge(THREADPOOL_THREADS_TOTAL, threadPoolExecutor.getCorePoolSize());
        metricService.submitGauge(THREADPOOL_QUEUE_SIZE, threadPoolExecutor.getQueue().size());
        metricService.submitGauge(THREADPOOL_TASKS_COMPLETED, threadPoolExecutor.getCompletedTaskCount());
        metricService.submitGauge(THREADPOOL_ACTIVE_THREADS, threadPoolExecutor.getActiveCount());
    }
}
