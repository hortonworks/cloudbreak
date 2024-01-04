package com.sequenceiq.cloudbreak.sigmadbus.processor;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.config.SigmaDatabusConfig;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.streaming.processor.AbstractRecordProcessor;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;
import com.sequenceiq.cloudbreak.telemetry.streaming.CommonStreamingConfiguration;

/**
 * Databus processor that holds processing queues that are used by round robin algorithm.
 * Sending new events to a blocking queue (round robin strategy), that will be picked up by a databus worker
 * (that will use the databus client to process the data)
 * The blocking queue passed to the databus record worker. (queue is used for data processing)
 * Implement this in order to process data into different databus streams.
 * @param <C> type of an implemented databus streaming configuration.
 */
public abstract class AbstractDatabusRecordProcessor<C extends AbstractDatabusStreamConfiguration>
        extends AbstractRecordProcessor<CommonStreamingConfiguration, DatabusRequest, DatabusRecordWorker<C>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDatabusRecordProcessor.class);

    private final SigmaDatabusConfig sigmaDatabusConfig;

    private final AtomicBoolean dataProcessingEnabled = new AtomicBoolean(false);

    private final C databusStreamConfiguration;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AbstractDatabusRecordProcessor(SigmaDatabusConfig sigmaDatabusConfig, C databusStreamConfiguration,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        super(databusStreamConfiguration);
        this.sigmaDatabusConfig = sigmaDatabusConfig;
        this.databusStreamConfiguration = databusStreamConfiguration;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @PostConstruct
    public void init() {
        String databusEndpoint = sigmaDatabusConfig.getEndpoint();
        if (StringUtils.isBlank(databusEndpoint) || !getDatabusStreamConfiguration().isEnabled()) {
            LOGGER.debug("Sigma DataBus endpoint is not set or stream processing is not enabled for {}", getDatabusStreamConfiguration().getDbusServiceName());
        } else {
            LOGGER.debug("Starting sigma databus worker for databus service: {} ", getDatabusStreamConfiguration().getDbusServiceName());
            dataProcessingEnabled.set(true);
        }
        super.init();
    }

    /**
     * Checks that data processing is enabled for databus. Disabled if there are no databus workers.
     */
    @Override
    public boolean isProcessingEnabled() {
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

    /**
     * RegionAwareInternalCrnGeneratorFactory
     */
    public RegionAwareInternalCrnGeneratorFactory getRegionAwareInternalCrnGeneratorFactory() {
        return regionAwareInternalCrnGeneratorFactory;
    }

    /**
     * Service name used to identify the record processor more easily
     */
    @Override
    public String getServiceName() {
        return databusStreamConfiguration.getDbusServiceName();
    }

    /**
     * Creating a new DatabusRecordWorker
     * @param threadName      thread name of the worker that is calculated by the round robin processing queue.
     * @param processingQueue processing queue that the worker watches.
     */
    @Override
    public DatabusRecordWorker<C> createWorker(String threadName, BlockingDeque<DatabusRequest> processingQueue) {
        return new DatabusRecordWorker<>(threadName, processingQueue, this, getRegionAwareInternalCrnGeneratorFactory());
    }

    /**
     * Check whether input is valid for processing.
     * @param input Input to be validated.
     */
    @Override
    public boolean isInputValid(DatabusRequest input) {
        return super.isInputValid(input) && doesAccountIdExist(input);
    }

    /**
     * Log a message about input not being valid.
     */
    @Override
    public void logInputIsNotValid() {
        LOGGER.debug("DataBusRecordInput needs both context with account ID and a payload message. Skip processing...");
    }

    /**
     * Override this to specify processor type name used in logs.
     */
    @Override
    public String getProcessorTypeForLog() {
        return "DataBus";
    }

    private boolean doesAccountIdExist(DatabusRequest input) {
        return input.getContext().isPresent() && StringUtils.isNotBlank(input.getContext().get().getAccountId());
    }

}
