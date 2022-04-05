package com.sequenceiq.cloudbreak.sigmadbus.processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

import io.opentracing.Tracer;

/**
 * Holds list of processing queues (blocking) and record worker (as pairs) for databus record processing.
 * It uses round robin scheduling algorithm for the data processing. (when putting record data into the blocking queue)
 * @param <C> type of a databus stream configuration.
 */
public class RoundRobinDatabusProcessingQueues<C extends AbstractDatabusStreamConfiguration>
        implements Iterable<BlockingDeque<DatabusRequest>> {

    private static final int DEFAULT_SIZE_LIMIT = 2000;

    private final List<BlockingDeque<DatabusRequest>> processingQueueList;

    private final List<DatabusRecordWorker<C>> workers;

    private final AbstractDatabusRecordProcessor<C> recordProcessor;

    private final AtomicInteger index = new AtomicInteger(0);

    private final int numberOfQueues;

    private final int sizeLimit;

    private final Tracer tracer;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public RoundRobinDatabusProcessingQueues(int numberOfQueues, int sizeLimit, AbstractDatabusRecordProcessor<C> recordProcessor,
        Tracer tracer, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.numberOfQueues = numberOfQueues > 0 ? numberOfQueues : 1;
        this.sizeLimit = sizeLimit > 0 ? sizeLimit : DEFAULT_SIZE_LIMIT;
        this.workers = new ArrayList<>();
        this.processingQueueList = new ArrayList<>();
        this.recordProcessor = recordProcessor;
        this.tracer = tracer;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
        initProcessingQueuesAndWorkers();
    }

    private void initProcessingQueuesAndWorkers() {
        for (int workerIndex = 0; workerIndex < this.numberOfQueues; workerIndex++) {
            String threadName = String.format("databus-%s-record-worker-%d",
                    recordProcessor.getDatabusStreamConfiguration().getDbusServiceName().toLowerCase(), workerIndex);
            BlockingDeque<DatabusRequest> processingQueue = new LinkedBlockingDeque<>();
            processingQueueList.add(processingQueue);
            DatabusRecordWorker<C> dataBusRecordWorker =
                    new DatabusRecordWorker<C>(threadName, processingQueue, recordProcessor, tracer, regionAwareInternalCrnGeneratorFactory);
            workers.add(dataBusRecordWorker);
        }
    }

    /**
     * Start databus record workers as daemons.
     */
    public void startWorkers() {
        for (DatabusRecordWorker<C> databusRecordWorker : workers) {
            databusRecordWorker.setDaemon(true);
            databusRecordWorker.start();
        }
    }

    /**
     * Put a record into a processing queue. Uses round robin scheduling for picking the queue (for the record location).
     */
    public void process(DatabusRequest input) throws InterruptedException {
        BlockingDeque<DatabusRequest> queue = iterator().next();
        if (queue.size() >= sizeLimit) {
            recordProcessor.handleDroppedDatabusRequest(input, sizeLimit);
        } else {
            queue.put(input);
        }
    }

    @VisibleForTesting
    List<BlockingDeque<DatabusRequest>> getProcessingQueueList() {
        return processingQueueList;
    }

    @Override
    public Iterator<BlockingDeque<DatabusRequest>> iterator() {
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public BlockingDeque<DatabusRequest> next() {
                int currentIndex = index.get();
                BlockingDeque<DatabusRequest> blockingDeque = processingQueueList.get(currentIndex);
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
