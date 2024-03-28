package com.sequenceiq.cloudbreak.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

class ThreadBasedUserCrnProviderTest {

    private static final int NUMBER_OF_TESTS = 1000;

    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Test
    public void testWhenVirtualThreadStartsNewThreadTheSubThreadUsesTheSameUserCrn() {
        List<Pair<String, Future<String>>> list = IntStream.range(0, NUMBER_OF_TESTS)
                .boxed()
                .map(i -> {
                    String userCrn = "crn:cdp:iam:us-west-1:accountId:user:" + i;
                    Future<String> future = submitSleepTask(userCrn, false);
                    return Pair.of(userCrn, future);
                })
                .toList();

        list.forEach(pair -> {
            try {
                String expectedUserCrn = pair.getKey();
                Future<String> future = pair.getValue();
                assertEquals(expectedUserCrn, future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testVirtualThreadCancellationDoesntCancelOtherThreads() throws InterruptedException {
        List<Triple<String, Boolean, Future<String>>> list = IntStream.range(0, 1000)
                .boxed()
                .map(i -> {
                    String userCrn = "crn:cdp:iam:us-west-1:accountId:user:" + i;
                    boolean shouldCancel = ThreadLocalRandom.current().nextBoolean();
                    Future<String> future = submitSleepTask(userCrn, shouldCancel);
                    return Triple.of(userCrn, shouldCancel, future);
                })
                .toList();

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        list.forEach(triple -> {
            boolean shouldCancel = triple.getMiddle();
            if (shouldCancel) {
                scheduledExecutorService.schedule(() -> {
                    triple.getRight().cancel(true);
                }, 1, TimeUnit.SECONDS);
            }
        });

        list.forEach(triple -> {
            boolean shouldCancel = triple.getMiddle();
            Future<String> future = triple.getRight();
            if (shouldCancel) {
                assertThrows(Exception.class, future::get);
            } else {
                String expectedUserCrn = triple.getLeft();
                try {
                    assertEquals(expectedUserCrn, future.get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        scheduledExecutorService.shutdown();
        scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    private Future<String> submitSleepTask(String userCrn, boolean willBeCancelled) {
        return ThreadBasedUserCrnProvider.doAs(userCrn, () -> executor.submit(() -> {
            Future<String> innerFuture = executor.submit(() -> {
                try {
                    if (willBeCancelled) {
                        TimeUnit.SECONDS.sleep(10);
                    } else {
                        TimeUnit.MILLISECONDS.sleep(100 + ThreadLocalRandom.current().nextInt(1000));
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return ThreadBasedUserCrnProvider.getUserCrn();
            });
            return innerFuture.get();
        }));
    }
}