package com.sequenceiq.cloudbreak.streaming.processor;

import java.util.concurrent.BlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.streaming.config.AbstractStreamingConfiguration;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;
import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;

/**
 * Worker class that should use specific/custom clients for record processing.
 * It process data in order from a blocking queue. Blocking queues and workers has a one-to-one relation.
 * @param <C> type of the streaming configuration.
 * @param <R> type of the request that is processed.
 */
public abstract class RecordWorker<P extends AbstractRecordProcessor, C extends AbstractStreamingConfiguration, R extends RecordRequest> extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRecordProcessor.class);

    private final BlockingDeque<R> processingQueue;

    private final String serviceName;

    private final P recordProcessor;

    private final C configuration;

    public RecordWorker(String name, String serviceName, P recordProcessor, BlockingDeque<R> processingQueue, C configuration) {
        super(name);
        this.serviceName = serviceName;
        this.recordProcessor = recordProcessor;
        this.processingQueue = processingQueue;
        this.configuration = configuration;
    }

    @Override
    public void run() {
        LOGGER.info("Start processing {} records. [name:{}]", serviceName, getName());
        while (true) {
            try {
                R input = processingQueue.take();
                try {
                    processRecordInput(input);
                } catch (StreamProcessingException e) {
                    LOGGER.warn("Unexpected error happened during data processing for {} service ", serviceName);
                    recordProcessor.handleDataStreamingException(input, e);
                } catch (Exception e) {
                    LOGGER.warn("Unexpected error happened during data processing for {} service ", serviceName);
                    recordProcessor.handleUnexpectedException(input, e);
                }
            } catch (InterruptedException ie) {
                onInterrupt();
                LOGGER.debug("{} record processing interrupted: {}", serviceName, ie.getMessage());
                break;
            }
        }
    }

    public C getConfiguration() {
        return configuration;
    }

    /**
     * Consumes a record from the blocking queue (FIFO) and processing it with a custom client.
     * @param input incoming record from the blocking queue
     * @throws StreamProcessingException throws this exception in case of any kind of error
     */
    public abstract void processRecordInput(R input) throws StreamProcessingException;

    /**
     * Triggered when the worker thread interrupted. It should cleanup resources or close clients.
     */
    public abstract void onInterrupt();
}