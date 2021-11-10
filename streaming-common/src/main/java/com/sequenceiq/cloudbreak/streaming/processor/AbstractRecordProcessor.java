package com.sequenceiq.cloudbreak.streaming.processor;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.streaming.config.AbstractStreamingConfiguration;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;
import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;

/**
 * Record processor that holds processing queues that are used by round robin algorithm.
 * Sending new events to a blocking queue (round robin strategy), that will be picked up by a record worker
 * (that can use a custom client to process the data)
 * The blocking queue passed to the record worker. (queue is used for data processing)
 * Implement this in order to process/stream data into queues that are processed by custom clients.
 * There can be different processors for different detinations that are controlled by custom configuraations.
 * @param <C> type of the streaming configuration.
 * @param <R> type of the request that is processed.
 * @param <W> type of the worker that implements the client specific mechanism for the processing.
 */
public abstract class AbstractRecordProcessor<C extends AbstractStreamingConfiguration, R extends RecordRequest, W extends RecordWorker> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRecordProcessor.class);

    private final AtomicBoolean processingEnabled;

    private final int numberOfWorkers;

    private final int queueSizeLimit;

    private final AtomicReference<RoundRobinStreamProcessingQueues<C, R, W>> processingQueuesRef;

    private final C configuration;

    public AbstractRecordProcessor(C configuration) {
        this.configuration = configuration;
        this.numberOfWorkers = configuration.getNumberOfWorkers();
        this.queueSizeLimit = configuration.getQueueSizeLimit();
        this.processingQueuesRef = new AtomicReference<>();
        this.processingEnabled = new AtomicBoolean(configuration.isEnabled());
    }

    /**
     * Put a record request into a blocking queue for processing.
     * @param input Record payload with request data (at least a grpc message or a raw string)
     */
    public void processRecord(R input) {
        if (isProcessingEnabled()) {
            try {
                if (messageIsNotEmpty(input)) {
                    getProcessingQueues().process(input);
                } else {
                    LOGGER.debug("Record request needs payload message. Skip processing...");
                }
            } catch (InterruptedException e) {
                LOGGER.debug("Putting record request for processing is interrupted. {}", e.getMessage());
            }
        } else {
            LOGGER.debug("{} processing is not enabled, skip text message processing.", getServiceName());
        }
    }

    /**
     * Call initialization for the record processor. The processor needs to be a spring component in order execute the initialization.
     * Initialization is skipped if the enabled flag is not set for processing or a re-implemented configuration validation returns false.
     */
    @PostConstruct
    public void init() {
        if (!isProcessingEnabled()) {
            LOGGER.debug("Processing is not enabled for {}", getServiceName());
        } else if (configuration == null || !validateConfiguration(configuration)) {
            LOGGER.debug("Configuration failed for {} processing.", getServiceName());
        } else {
            LOGGER.debug("Starting record worker for {} processing service.", getServiceName());
            RoundRobinStreamProcessingQueues<C, R, W> processingQueues = new RoundRobinStreamProcessingQueues<>(
                    numberOfWorkers, queueSizeLimit, this);
            processingQueuesRef.set(processingQueues);
            getProcessingQueues().startWorkers();
        }
    }

    /**
     * Creating a new custom worker (client operations needs to be implemented in the worker)
     * @param threadName thread name of the worker that is calculsted by the round robin processing queue.
     * @param processingQueue processing queue that the worker watches.
     * @return newly created worker
     */
    public abstract W createWorker(String threadName, BlockingDeque<R> processingQueue);

    /**
     * Name that represents the processing service type. It should be unique for every processor.
     */
    public abstract String getServiceName();

    /**
     * Override this to change the default behavior when the processing queue is too high.
     * Default behaviour: log the dropped record request input.
     */
    public void handleDroppedRecordRequest(R input, int sizeLimit) {
        LOGGER.warn("Blocking queue reached size limit: {}. Dropping record input: {}", sizeLimit, input);
    }

    /**
     * Override this to change the default behavior when an stream record processing error happens.
     * Default behaviour: log the exception.
     */
    public void handleDataStreamingException(R input, StreamProcessingException e) {
        LOGGER.warn(String.format("Exception during stream record processing [skip] - input: %s", input), e);
    }

    /**
     * Override this to change the default behavior when an unexpected error happens.
     * Default behaviour: log the exception.
     */
    public void handleUnexpectedException(R input, Exception e) {
        LOGGER.warn(String.format("Unexpected exception during stream record processing [skip] - input: %s", input), e);
    }

    public boolean isProcessingEnabled() {
        return processingEnabled.get();
    }

    public RoundRobinStreamProcessingQueues<C, R, W> getProcessingQueues() {
        return processingQueuesRef.get();
    }

    /**
     * Validate provided custom configuration.
     * @param configuration object that holds global configs for the processor.
     * @return validation result
     */
    public boolean validateConfiguration(C configuration) {
        return true;
    }

    public C getConfiguration() {
        return configuration;
    }

    private boolean messageIsNotEmpty(R input) {
        return input != null &&
                (rawMessageIsNotEmpty(input) || input.getMessageBody().isPresent());
    }

    private boolean rawMessageIsNotEmpty(R input) {
        return input.getRawBody().isPresent() && StringUtils.isNotBlank(input.getRawBody().get());
    }
}
