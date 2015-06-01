package com.sequenceiq.cloudbreak;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class TestSchedularService {
    long sleep = 2;

    public static void main(String[] args) throws Exception {
        new TestSchedularService().testLoop();
    }

    public void testLoop() throws Exception {

        ListeningScheduledExecutorService l = MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(10));
        ListenableScheduledFuture<Result> future = l.schedule(new PollingService(), 0, TimeUnit.SECONDS);
        Futures.addCallback(future, new FutureCallback<Result>() {


            @Override
            public void onSuccess(Result result) {
                // we want this handler to run immediately after we push the big red button!
                System.out.println(result.getResult());
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });

        Thread.sleep(10 * sleep * 1000);
        future.cancel(false);
        l.shutdown();
    }
}

class PollingService implements Callable<Result> {
    private int count = 0;

    @Override
    public Result call() throws Exception {
        count++;
        Thread.sleep(1000);
        return new Result("iteration :" + count++);
    }
}

class Result {

    private String result;

    public Result(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}