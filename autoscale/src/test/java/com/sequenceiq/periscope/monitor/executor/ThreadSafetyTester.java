package com.sequenceiq.periscope.monitor.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntConsumer;

public class ThreadSafetyTester {

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private int numberOfThreads;

    private IntConsumer toTest;

    private List<Future> futures = new ArrayList<>();

    private ExecutorService executorService;

    public ThreadSafetyTester(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
    }

    public ThreadSafetyTester withBlockToTest(IntConsumer toTest) {
        this.toTest = toTest;
        return this;
    }

    public ThreadSafetyTester run() {
        for (int i = 0; i < numberOfThreads; i++) {
            final int valueToSupply = i;
            Future future = executorService.submit(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                }

                toTest.accept(valueToSupply);
            });
            futures.add(future);
        }

        countDownLatch.countDown();
        return this;
    }

    public void waitUntilFinished() {
        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            }
        });
    }
}
