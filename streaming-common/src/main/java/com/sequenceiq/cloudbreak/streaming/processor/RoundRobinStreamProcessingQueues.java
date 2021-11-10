package com.sequenceiq.cloudbreak.streaming.processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequenceiq.cloudbreak.streaming.config.AbstractStreamingConfiguration;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;

/**
 * Holds list of processing queues (blocking) and record worker (as pairs) for record processing.
 * It uses round robin scheduling algorithm for the data processing. (when putting record data into the blocking queue)
 * @param <C> type of the streaming configuration.
 * @param <R> type of the request that is processed.
 * @param <W> type of the worker that implements the client specific mechanism for the processing.
 */
public class RoundRobinStreamProcessingQueues
        <C extends AbstractStreamingConfiguration, R extends RecordRequest, W extends RecordWorker> implements Iterable<BlockingDeque<R>> {

    private static final int DEFAULT_SIZE_LIMIT = 2000;

    private final List<BlockingDeque<R>> processingQueueList;

    private final List<W> workers;

    private final AbstractRecordProcessor<C, R, W> recordProcessor;

    private final AtomicInteger index = new AtomicInteger(0);

    private final int numberOfQueues;

    private final int sizeLimit;

    public RoundRobinStreamProcessingQueues(int numberOfQueues, int sizeLimit, AbstractRecordProcessor<C, R, W> recordProcessor) {
        this.numberOfQueues = numberOfQueues > 0 ? numberOfQueues : 1;
        this.sizeLimit = sizeLimit > 0 ? sizeLimit : DEFAULT_SIZE_LIMIT;
        this.workers = new ArrayList<>();
        this.processingQueueList = new ArrayList<>();
        this.recordProcessor = recordProcessor;
        initProcessingQueuesAndWorkers();
    }

    private void initProcessingQueuesAndWorkers() {
        for (int workerIndex = 0; workerIndex < this.numberOfQueues; workerIndex++) {
            String threadName = String.format("%s-record-worker-%d", recordProcessor.getServiceName().toLowerCase(), workerIndex);
            BlockingDeque<R> processingQueue = new LinkedBlockingDeque<>();
            processingQueueList.add(processingQueue);
            W recordWorker = recordProcessor.createWorker(threadName, processingQueue);
            workers.add(recordWorker);
        }
    }

    /**
     * Start record workers as daemons.
     */
    public void startWorkers() {
        for (W recordWorker : workers) {
            recordWorker.setDaemon(true);
            recordWorker.start();
        }
    }

    /**
     * Put a record into a processing queue. Uses round robin scheduling for picking the queue (for the record location).
     */
    public void process(R input) throws InterruptedException {
        BlockingDeque<R> queue = iterator().next();
        if (queue.size() >= sizeLimit) {
            recordProcessor.handleDroppedRecordRequest(input, sizeLimit);
        } else {
            queue.put(input);
        }
    }

    List<BlockingDeque<R>> getProcessingQueueList() {
        return processingQueueList;
    }

    @Override
    public Iterator<BlockingDeque<R>> iterator() {
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public BlockingDeque<R> next() {
                int currentIndex = index.get();
                BlockingDeque<R> blockingDeque = processingQueueList.get(currentIndex);
                int newIndex = (currentIndex + 1) % numberOfQueues;
                index.set(newIndex);
                return blockingDeque;
            }

            @Override
            public void remove() {
                throw new RuntimeException("Cannot remove elements from round robin list.");
            }
        };
    }
}
