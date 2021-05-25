package com.sequenceiq.cdp.databus.processor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cdp.databus.client.DatabusClient;
import com.sequenceiq.cdp.databus.client.DatabusApiRetryHandler;
import com.sequenceiq.cdp.databus.model.DatabusRecordInput;
import com.sequenceiq.cdp.databus.model.DatabusRequestContext;
import com.sequenceiq.cdp.databus.model.exception.DatabusRecordProcessingException;
import com.sequenceiq.cdp.databus.service.AccountDatabusConfigService;
import com.sequenceiq.cdp.databus.tracing.DatabusClientTracingFeatureFactory;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

/**
 * Databus processor that holds round robin processing queue (contains an in-memory cache and blocking queue(s) with record processors).
 * Sending new events to a blocking queue (round robin strategy), that will be picked up by a databus worker
 * (that will use the databus client to process the data)
 * The blocking queue and in-memory cache is passed to the databus record worker.
 * (queue is used for data processing, the cache is used to avoid frequent database operations)
 * Implement this in order to process data into different databus streams.
 * @param <D> type of an implemented databus streaming configuration.
 */
public abstract class AbstractDatabusRecordProcessor<D extends AbstractDatabusStreamConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDatabusRecordProcessor.class);

    private final AltusDatabusConfiguration altusDatabusConfiguration;

    private final AtomicBoolean dataProcessingEnabled = new AtomicBoolean(false);

    private final AccountDatabusConfigService accountDatabusConfigService;

    private final D databusStreamConfiguration;

    private final int numberOfWorkers;

    private final int queueSizeLimit;

    private final AtomicReference<RoundRobinDatabusProcessingQueues<D>> processingQueuesRef;

    @Inject
    private DatabusClientTracingFeatureFactory databusClientTracingFeatureFactory;

    public AbstractDatabusRecordProcessor(AccountDatabusConfigService accountDatabusConfigService,
            AltusDatabusConfiguration altusDatabusConfiguration, D databusStreamConfiguration,
            int numberOfWorkers, int queueSizeLimit) {
        this.accountDatabusConfigService = accountDatabusConfigService;
        this.altusDatabusConfiguration = altusDatabusConfiguration;
        this.databusStreamConfiguration = databusStreamConfiguration;
        this.numberOfWorkers = numberOfWorkers;
        this.queueSizeLimit = queueSizeLimit;
        this.processingQueuesRef = new AtomicReference<>();
    }

    @PostConstruct
    public void init() {
        String databusEndpoint = altusDatabusConfiguration.getAltusDatabusEndpoint();
        if (StringUtils.isBlank(databusEndpoint) || !getDatabusStreamConfiguration().isEnabled()) {
            LOGGER.debug("DataBus endpoint is not set or stream processing is not enabled for {}", getDatabusStreamConfiguration().getDbusServiceName());
        } else {
            LOGGER.debug("Starting databus worker for databus service: {} ", getDatabusStreamConfiguration().getDbusServiceName());
            RoundRobinDatabusProcessingQueues<D> processingQueues = new RoundRobinDatabusProcessingQueues<D>(numberOfWorkers, queueSizeLimit,
                    createClientBuilder(), accountDatabusConfigService, this);
            processingQueuesRef.set(processingQueues);
            getProcessingQueues().startWorkers();
            dataProcessingEnabled.set(true);
        }
    }

    /**
     * Transform a GRPC object to JSON string and put that into the blocking queue for processing.
     * @param grpcPayload  GRPC input object
     * @param context      request context of the databus input
     */
    public void processRecord(GeneratedMessageV3 grpcPayload, DatabusRequestContext context) {
        try {
            if (isDatabusProcessingEnabled()) {
                processRecord(JsonFormat.printer().print(grpcPayload), context);
            } else {
                LOGGER.debug("Databus processing is not enabled, skip grpc message processing to databus");
            }
        } catch (InvalidProtocolBufferException e) {
            LOGGER.debug("Cannot parse grcp message input: {}", grpcPayload);
        }
    }

    /**
     * Use a string message as a payload and put that into the blocking queue for processing.
     * Also removes endlines from the JSON input.
     * @param body      payload string
     * @param context   request context of the databus input
     */
    public void processRecord(String body, DatabusRequestContext context) {
        if (isDatabusProcessingEnabled()) {
            String payloadWithoutLineBreaks = body.replace("\\n", "");
            processRecord(DatabusRecordInput.Builder.builder()
                    .withDatabusRequestContext(context)
                    .withPayload(payloadWithoutLineBreaks)
                    .withDatabusStreamConfiguration(getDatabusStreamConfiguration())
                    .build());
        } else {
            LOGGER.debug("Databus processing is not enabled, skip text message processing to databus");
        }
    }

    private void processRecord(DatabusRecordInput input) {
        try {
            if (input.getDatabusRequestContext().isPresent() && input.getRecordRequest().isPresent()) {
                getProcessingQueues().process(input);
            } else {
                LOGGER.debug("DataBusRecordInput needs both account ID and a payload. Skip processing...");
            }
        } catch (InterruptedException e) {
            LOGGER.debug("Putting DataBus put record request for processing is interrupted. {}", e.getMessage());
        }
    }

    /**
     * Override this to change the default behavior when an databus record processing error happens.
     * Default behaviour: log the exception.
     */
    protected void handleDatabusRecordProcessingException(DatabusRecordInput input, String machineUserName,
            String accountId, DatabusRecordProcessingException e) {
        LOGGER.debug(String.format("Exception during databus record processing [skip] - [accountId: %s, machineUser: %s] record: %s",
                accountId, machineUserName, input), e);
    }

    /**
     * Override this to change the default behavior when an unexpected error happens.
     * Default behaviour: log the exception.
     */
    protected void handleUnexpectedException(DatabusRecordInput input, String machineUserName,
            String accountId, Exception e) {
        LOGGER.debug(String.format("Unexpected exception during databus record processing - [accountId: %s, machineUser: %s] record: %s",
                accountId, machineUserName, input), e);
    }

    /**
     * This method can be used to overriding the databus client settings that will be created by the databus worker.
     */
    protected DatabusClient.Builder extendDatabusClientBuilder(DatabusClient.Builder builder) {
        return builder;
    }

    /**
     * Use retry handler in case of server (unavailable) errors. Override this method to enable the retries.
     * If there are a lot of records in the blocking queue, that can cause more and more memory usage,
     * but the processing will more fault tolerant.
     */
    protected boolean retryOnServiceUnavailableError() {
        return false;
    }

    /**
     * Machine user name that will be stored in the database and in UMS and used by the databus worker for data processing.
     */
    public String getMachineUserName(String accountId) {
        return getAccountMachineUserNamePrefix() + "-" + accountId;
    }

    /**
     * Round robin processing queue list that put data into the queue and record workers will pick those up.
     */
    public RoundRobinDatabusProcessingQueues<D> getProcessingQueues() {
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
    public D getDatabusStreamConfiguration() {
        return this.databusStreamConfiguration;
    }

    /**
     * Provide a machine user name prefix. This name will be used in UMS and in the database with an account suffix.
     * e.g.: prefix: mymachineuser -> mymachineuser-[accountid]
     */
    public abstract String getAccountMachineUserNamePrefix();

    private DatabusClient.Builder createClientBuilder() {
        DatabusClient.Builder builder = DatabusClient.builder();
        if (StringUtils.isNotBlank(altusDatabusConfiguration.getAltusDatabusEndpoint())) {
            builder.withClientTracingFeature(databusClientTracingFeatureFactory.createClientTracingFeature());
        }
        builder.withRetryHandler(DatabusApiRetryHandler.Builder.builder()
                .withRetryOnServerUnavailable(retryOnServiceUnavailableError())
                .build());
        return extendDatabusClientBuilder(builder.withEndpoint(altusDatabusConfiguration.getAltusDatabusEndpoint()));
    }
}
