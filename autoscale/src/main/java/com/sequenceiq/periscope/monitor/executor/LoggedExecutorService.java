package com.sequenceiq.periscope.monitor.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.utils.LoggerUtils;
import com.sequenceiq.periscope.utils.MetricUtils;

@Service
public class LoggedExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggedExecutorService.class);

    @Inject
    @Qualifier("periscopeListeningScheduledExecutorService")
    private ExecutorService executorService;

    @Inject
    private LoggerUtils loggerUtils;

    @Inject
    private MetricUtils metricUtils;

    public Future<?> submit(String name, Runnable task) {
        try {
            return executorService.submit(task);
        } finally {
            loggerUtils.logThreadPoolExecutorParameters(LOGGER, name, executorService);
            metricUtils.submitThreadPoolExecutorParameters(executorService);
        }
    }
}
