package com.sequenceiq.cloudbreak.sigmadbus.processor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.config.SigmaDatabusConfig;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRecordProcessingException;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

import io.opentracing.Tracer;

/**
 * Databus processor that holds processing queues that are used by round robin algorithm.
 * Sending new events to a blocking queue (round robin strategy), that will be picked up by a databus worker
 * (that will use the databus client to process the data)
 * The blocking queue passed to the databus record worker. (queue is used for data processing)
 * Implement this in order to process data into different databus streams.
 * @param <C> type of an implemented databus streaming configuration.
 */
public abstract class AbstractDatabusRecordProcessor<C extends AbstractDatabusStreamConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDatabusRecordProcessor.class);

    private final SigmaDatabusConfig sigmaDatabusConfig;

    private final AtomicBoolean dataProcessingEnabled = new AtomicBoolean(false);

    private final C databusStreamConfiguration;

    private final int numberOfWorkers;

    private final int queueSizeLimit;

    private final AtomicReference<RoundRobinDatabusProcessingQueues<C>> processingQueuesRef;

    private final Tracer tracer;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AbstractDatabusRecordProcessor(SigmaDatabusConfig sigmaDatabusConfig, C databusStreamConfiguration,
            int numberOfWorkers, int queueSizeLimit, Tracer tracer, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.sigmaDatabusConfig = sigmaDatabusConfig;
        this.databusStreamConfiguration = databusStreamConfiguration;
        this.numberOfWorkers = numberOfWorkers;
        this.queueSizeLimit = queueSizeLimit;
        this.processingQueuesRef = new AtomicReference<>();
        this.tracer = tracer;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @PostConstruct
    public void init() {
        String databusEndpoint = sigmaDatabusConfig.getEndpoint();
        if (StringUtils.isBlank(databusEndpoint) || !getDatabusStreamConfiguration().isEnabled()) {
            LOGGER.debug("Sigma DataBus endpoint is not set or stream processing is not enabled for {}", getDatabusStreamConfiguration().getDbusServiceName());
        } else {
            LOGGER.debug("Starting sigma databus worker for databus service: {} ", getDatabusStreamConfiguration().getDbusServiceName());
            RoundRobinDatabusProcessingQueues<C> processingQueues = new RoundRobinDatabusProcessingQueues<C>(
                    numberOfWorkers, queueSizeLimit, this, tracer, regionAwareInternalCrnGeneratorFactory);
            processingQueuesRef.set(processingQueues);
            getProcessingQueues().startWorkers();
            dataProcessingEnabled.set(true);
        }
    }

    /**
     * Put a databus record request into a blocking queue for processing.
     * @param input Databus record payload with request context
     */
    public void processRecord(DatabusRequest input) {
        if (isDatabusProcessingEnabled()) {
            try {
                if (messageIsNotEmpty(input) && doesAccountIdExist(input)) {
                    getProcessingQueues().process(input);
                } else {
                    LOGGER.debug("DataBusRecordInput needs both context with account ID and a payload message. Skip processing...");
                }
            } catch (InterruptedException e) {
                LOGGER.debug("Putting DataBus put record request for processing is interrupted. {}", e.getMessage());
            }
        } else {
            LOGGER.debug("Databus processing is not enabled, skip text message processing to databus");
        }
    }

    /**
     * Override this to change the default behavior when an databus record processing error happens.
     * Default behaviour: log the exception.
     */
    protected void handleDatabusRecordProcessingException(DatabusRequest input, DatabusRecordProcessingException e) {
        LOGGER.warn(String.format("Exception during databus record processing [skip] - input: %s", input), e);
    }

    /**
     * Override this to change the default behavior when an unexpected error happens.
     * Default behaviour: log the exception.
     */
    protected void handleUnexpectedException(DatabusRequest input, Exception e) {
        LOGGER.warn(String.format("Unexpected exception during databus record processing [skip] - input: %s", input), e);
    }

    /**
     * Override this to change the default behavior when the processing queue is too high.
     * Default behaviour: log the dropped databus request input.
     */
    protected void handleDroppedDatabusRequest(DatabusRequest input, int sizeLimit) {
        LOGGER.warn("Blocking queue reached size limit: {}. Dropping databus input: {}", sizeLimit, input);
    }

    /**
     * Round robin processing queue list that put data into the queue and record workers will pick those up.
     */
    public RoundRobinDatabusProcessingQueues<C> getProcessingQueues() {
        return processingQueuesRef.get();
    }

    /**
     * Checks that data processing is enabled for databus. Disabled if there are no databus workers.
     */
    public boolean isDatabusProcessingEnabled() {
        return this.dataProcessingEnabled.get();
    }

    /**
     * Provide a valid (properly implemented) databus stream configuration.
     */
    public C getDatabusStreamConfiguration() {
        return this.databusStreamConfiguration;
    }

    /**
     * Sigma dbus (grpc) related configuration
     */
    public SigmaDatabusConfig getSigmaDatabusConfig() {
        return this.sigmaDatabusConfig;
    }

    private boolean messageIsNotEmpty(DatabusRequest input) {
        return input != null &&
                (rawMessageIsNotEmpty(input) || input.getMessageBody().isPresent());
    }

    private boolean rawMessageIsNotEmpty(DatabusRequest input) {
        return input.getRawBody().isPresent() && StringUtils.isNotBlank(input.getRawBody().get());
    }

    private boolean doesAccountIdExist(DatabusRequest input) {
        return input.getContext().isPresent() && StringUtils.isNotBlank(input.getContext().get().getAccountId());
    }

}
