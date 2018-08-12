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

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private final int numberOfThreads;

    private IntConsumer toTest;

    private final List<Future<?>> futures = new ArrayList<>();

    private final ExecutorService executorService;

    ThreadSafetyTester(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
    }

    ThreadSafetyTester withBlockToTest(IntConsumer toTest) {
        this.toTest = toTest;
        return this;
    }

    public ThreadSafetyTester run() {
        for (int i = 0; i < numberOfThreads; i++) {
            int valueToSupply = i;
            Future<?> future = executorService.submit(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException ignored) {
                }

                toTest.accept(valueToSupply);
            });
            futures.add(future);
        }

        countDownLatch.countDown();
        return this;
    }

    void waitUntilFinished() {
        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        });
    }
}
