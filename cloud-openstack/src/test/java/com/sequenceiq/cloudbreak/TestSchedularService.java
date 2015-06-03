package com.sequenceiq.cloudbreak;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class TestSchedularService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestSchedularService.class);
    private static final int CORE_POOL_SIZE = 10;
    private static final int SLEEP_COUNT = 10;
    private static final int SEC_IN_MILLIS = 1000;
    private long sleep = 2;

    public static void main(String[] args) throws Exception {
        new TestSchedularService().testLoop();
    }

    public void testLoop() throws Exception {

        ListeningScheduledExecutorService l = MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(CORE_POOL_SIZE));
        ListenableScheduledFuture<Result> future = l.schedule(new PollingService(), 0, TimeUnit.SECONDS);
        Futures.addCallback(future, new FutureCallback<Result>() {

            @Override
            public void onSuccess(Result result) {
                // we want this handler to run immediately after we push the big red button!
                LOGGER.info("Result: {}", result.getResult());
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });

        Thread.sleep(SLEEP_COUNT * sleep * SEC_IN_MILLIS);
        future.cancel(false);
        l.shutdown();
    }
}


