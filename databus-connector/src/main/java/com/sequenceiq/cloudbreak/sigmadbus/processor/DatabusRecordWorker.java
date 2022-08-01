package com.sequenceiq.cloudbreak.sigmadbus.processor;

import java.util.concurrent.BlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.SigmaDatabusClient;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;
import com.sequenceiq.cloudbreak.streaming.processor.RecordWorker;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;
import com.sequenceiq.cloudbreak.telemetry.streaming.CommonStreamingConfiguration;

import io.opentracing.Tracer;

/**
 * Worker class that uses sigma databus client for record processing. (with put record operation)
 * It process data in order from a blocking queue. Blocking queues and workers has a one-to-one relation.
 * @param <C> type of a databus streaming configuration.
 */
public class DatabusRecordWorker<C extends AbstractDatabusStreamConfiguration>
        extends RecordWorker<AbstractDatabusRecordProcessor<C>, CommonStreamingConfiguration, DatabusRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabusRecordWorker.class);

    private final Tracer tracer;

    private SigmaDatabusClient<C> dataBusClient;

    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public DatabusRecordWorker(String name, BlockingDeque<DatabusRequest> processingQueue,
            AbstractDatabusRecordProcessor<C> databusRecordProcessor, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        super(name, name, databusRecordProcessor, processingQueue, null);
        this.tracer = tracer;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public SigmaDatabusClient<C> getClient() {
        if (dataBusClient == null) {
            dataBusClient = new SigmaDatabusClient<C>(tracer, getRecordProcessor().getSigmaDatabusConfig(),
                    getRecordProcessor().getDatabusStreamConfiguration(), regionAwareInternalCrnGeneratorFactory);
        }
        return dataBusClient;
    }

    @Override
    public void run() {
        LOGGER.info("Start processing databus records. [name:{}]", getName());
        while (true) {
            try {
                processRecordInput(getProcessingQueue().take());
            } catch (InterruptedException ie) {
                if (dataBusClient != null) {
                    dataBusClient.close();
                }
                LOGGER.debug("DataBus record processing interrupted: {}", ie.getMessage());
                break;
            }
        }
    }

    @VisibleForTesting
    @Override
    public void processRecordInput(DatabusRequest input) {
        try {
            getClient().putRecord(input);
        } catch (StreamProcessingException ex) {
            getRecordProcessor().handleDataStreamingException(input, ex);
        } catch (Exception ex) {
            getRecordProcessor().handleUnexpectedException(input, ex);
        }
    }

    @Override
    public void onInterrupt() {
    }
}
