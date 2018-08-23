package com.sequenceiq.periscope.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class BlockingTaskManager {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private final Map<Integer, BlockingTask> blockingTasks = new ConcurrentHashMap<>();

    private final AtomicBoolean releasedAll = new AtomicBoolean(false);

    BlockingTask getNewTask() {
        if (releasedAll.get()) {
            throw new RuntimeException("cannot create blocking tasks anymore");
        }

        BlockingTask blockingTask = new BlockingTask(countDownLatch);
        blockingTasks.put(NEXT_ID.incrementAndGet(), blockingTask);
        return blockingTask;
    }

    BlockingTaskFinisher releaseAll() {
        if (releasedAll.compareAndSet(false, true)) {
            countDownLatch.countDown();
            return new BlockingTaskFinisher(blockingTasks);
        }

        throw new RuntimeException("BlockingTaskManager was already released");
    }

    public static class BlockingTaskFinisher {

        private final Map<Integer, BlockingTask> blockingTasks;

        private BlockingTaskFinisher(Map<Integer, BlockingTask> blockingTasks) {
            this.blockingTasks = blockingTasks;
        }

        void waitAll() {
            boolean finished = blockingTasks.values().stream().allMatch(BlockingTask::isFinished);
            while (!finished) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                finished = blockingTasks.values().stream().allMatch(BlockingTask::isFinished);
            }
        }
    }

    public static class BlockingTask implements Runnable {

        private final CountDownLatch countDownLatch;

        private boolean finished;

        private BlockingTask(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        private boolean isFinished() {
            return finished;
        }

        @Override
        public void run() {
            try {
                countDownLatch.await();
            } catch (InterruptedException ignored) {
            } finally {
                finished = true;
            }
        }
    }
}
