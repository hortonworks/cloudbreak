package com.sequenceiq.cloudbreak.sigmadbus.processor;

import java.util.concurrent.BlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.SigmaDatabusClient;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRecordProcessingException;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

import io.opentracing.Tracer;

/**
 * Worker class that uses sigma databus client for record processing. (with put record operation)
 * It process data in order from a blocking queue. Blocking queues and workers has a one-to-one relation.
 * @param <C> type of a databus streaming configuration.
 */
public class DatabusRecordWorker<C extends AbstractDatabusStreamConfiguration> extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabusRecordWorker.class);

    private final Tracer tracer;

    private final BlockingDeque<DatabusRequest> processingQueue;

    private final AbstractDatabusRecordProcessor<C> databusRecordProcessor;

    private SigmaDatabusClient<C> dataBusClient;

    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public DatabusRecordWorker(String name, BlockingDeque<DatabusRequest> processingQueue,
            AbstractDatabusRecordProcessor<C> databusRecordProcessor, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        super(name);
        this.tracer = tracer;
        this.processingQueue = processingQueue;
        this.databusRecordProcessor = databusRecordProcessor;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public SigmaDatabusClient<C> getClient() {
        if (dataBusClient == null) {
            dataBusClient = new SigmaDatabusClient<C>(tracer, databusRecordProcessor.getSigmaDatabusConfig(),
                    databusRecordProcessor.getDatabusStreamConfiguration(), regionAwareInternalCrnGeneratorFactory);
        }
        return dataBusClient;
    }

    @Override
    public void run() {
        LOGGER.info("Start processing databus records. [name:{}]", getName());
        while (true) {
            try {
                processRecordInput(processingQueue.take());
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
    void processRecordInput(DatabusRequest request) {
        try {
            getClient().putRecord(request);
        } catch (DatabusRecordProcessingException ex) {
            databusRecordProcessor.handleDatabusRecordProcessingException(request, ex);
        } catch (Exception ex) {
            databusRecordProcessor.handleUnexpectedException(request, ex);
        }
    }
}
