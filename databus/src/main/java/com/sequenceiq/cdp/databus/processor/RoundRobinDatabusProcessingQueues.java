package com.sequenceiq.cdp.databus.processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cdp.databus.cache.AccountDatabusConfigCache;
import com.sequenceiq.cdp.databus.client.DatabusClient;
import com.sequenceiq.cdp.databus.model.DatabusRecordInput;
import com.sequenceiq.cdp.databus.service.AccountDatabusConfigService;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

/**
 * Holds list of processing queues (blocking) and record worker (as pairs) for databus record processing
 * and a shared cache for storing account level databus credentials.
 * It uses round robin scheduling algorithm for processing. (when putting record data into the blocking queue)
 * @param <C> type of a databus stream configuration.
 */
public class RoundRobinDatabusProcessingQueues<C extends AbstractDatabusStreamConfiguration>
        implements Iterable<BlockingDeque<DatabusRecordInput>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoundRobinDatabusProcessingQueues.class);

    private static final int DEFAULT_SIZE_LIMIT = 2000;

    private final List<BlockingDeque<DatabusRecordInput>> processingQueueList;

    private final List<DatabusRecordWorker> workers;

    private final AccountDatabusConfigCache accountDatabusConfigCache = new AccountDatabusConfigCache();

    private final AtomicInteger index = new AtomicInteger(0);

    private final int numberOfQueues;

    private final int sizeLimit;

    public RoundRobinDatabusProcessingQueues(int numberOfQueues, int sizeLimit, DatabusClient.Builder clientBuilder,
            AccountDatabusConfigService accountDatabusConfigService, AbstractDatabusRecordProcessor<C> recordProcessor) {
        this.numberOfQueues = numberOfQueues > 0 ? numberOfQueues : 1;
        this.sizeLimit = sizeLimit > 0 ? sizeLimit : DEFAULT_SIZE_LIMIT;
        this.workers = new ArrayList<>();
        this.processingQueueList = new ArrayList<>();
        initProcessingQueuesAndWowkers(clientBuilder, accountDatabusConfigService, recordProcessor);
    }

    private void initProcessingQueuesAndWowkers(DatabusClient.Builder clientBuilder, AccountDatabusConfigService accountDatabusConfigService,
            AbstractDatabusRecordProcessor<C> recordProcessor) {
        for (int workerIndex = 0; workerIndex < this.numberOfQueues; workerIndex++) {
            String threadName = String.format("databus-%s-record-worker-%d",
                    recordProcessor.getDatabusStreamConfiguration().getDbusServiceName().toLowerCase(), workerIndex);
            BlockingDeque<DatabusRecordInput> processingQueue = new LinkedBlockingDeque<>();
            processingQueueList.add(processingQueue);
            DatabusRecordWorker dataBusRecordWorker = new DatabusRecordWorker(threadName, clientBuilder, accountDatabusConfigService,
                    accountDatabusConfigCache, processingQueue, recordProcessor);
            workers.add(dataBusRecordWorker);
        }
    }

    /**
     * Start databus record workers as daemons.
     */
    public void startWorkers() {
        for (DatabusRecordWorker databusRecordWorker : workers) {
            databusRecordWorker.setDaemon(true);
            databusRecordWorker.start();
        }
    }

    /**
     * Put a record into a processing queue. Uses round robin scheduling for picking the queue (for the record location).
     */
    public void process(DatabusRecordInput input) throws InterruptedException {
        BlockingDeque<DatabusRecordInput> queue = iterator().next();
        if (queue.size() >= sizeLimit) {
            LOGGER.debug("Blocking queue reached size limit: {}. Dropping databus input: {}", sizeLimit, input);
        } else {
            queue.put(input);
        }
    }

    @VisibleForTesting
    List<BlockingDeque<DatabusRecordInput>> getProcessingQueueList() {
        return processingQueueList;
    }

    @Override
    public Iterator<BlockingDeque<DatabusRecordInput>> iterator() {
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public BlockingDeque<DatabusRecordInput> next() {
                int currentIndex = index.get();
                BlockingDeque<DatabusRecordInput> blockingDeque = processingQueueList.get(currentIndex);
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
