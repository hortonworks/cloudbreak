package com.sequenceiq.cloudbreak.cloud.scheduler;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import reactor.bus.EventBus;

@Component
@Scope(value = "prototype")
public class ListenablePollingScheduler implements Runnable, FutureCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenablePollingScheduler.class);

    @Inject
    private ListeningScheduledExecutorService scheduler;

    @Inject
    private EventBus eventBus;

    private CountDownLatch latch;

    private ListenableScheduledFuture selfSchedule;

    public void schedule(int count, int period) {
        this.latch = new CountDownLatch(count);
        selfSchedule = scheduler.scheduleAtFixedRate(this, 0, period, TimeUnit.SECONDS);
        Futures.addCallback(selfSchedule, this);
    }

    public void cancel() {
        selfSchedule.cancel(false);
    }

    @Override
    public void run() {
        latch.countDown();
        LOGGER.info("Schedule received: {}, count: {}", this, latch.getCount());
        if (latch.getCount() == 0) {
            cancel();
        }
    }

    @Override
    public void onSuccess(Object result) {
        LOGGER.info("Success: ", result);
    }

    @Override
    public void onFailure(Throwable t) {
        if (t instanceof CancellationException) {
            if (latch.getCount() == 0) {
                LOGGER.debug("Scheduler task finished!");
            } else {
                LOGGER.debug("Scheduler task has been cancelled!");
            }

        } else {
            LOGGER.info("Failure reason: ", t);
        }
    }
}
