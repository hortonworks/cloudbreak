package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Component
public class SchedulerProvider {

    private final Scheduler ioScheduler;

    public SchedulerProvider(@Qualifier("azureClientThreadPool") ExecutorService mdcCopyingThreadPoolExecutor) {
        ioScheduler = Schedulers.fromExecutorService(mdcCopyingThreadPoolExecutor);
    }

    public Scheduler io() {
        return ioScheduler;
    }
}
