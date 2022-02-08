package com.sequenceiq.cloudbreak.usage.http;

import java.util.concurrent.BlockingDeque;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;
import com.sequenceiq.cloudbreak.streaming.processor.AbstractRecordProcessor;
import com.sequenceiq.cloudbreak.usage.strategy.LoggingUsageProcessingStrategy;

import io.opentracing.Tracer;

@Component
public class UsageHttpRecordProcessor extends AbstractRecordProcessor<UsageHttpConfiguration, UsageHttpRecordRequest, UsageHttpRecordWorker> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsageHttpRecordProcessor.class);

    private final LoggingUsageProcessingStrategy loggingUsageProcessingStrategy;

    private final Tracer tracer;

    public UsageHttpRecordProcessor(EdhHttpConfiguration edhHttpConfiguration, LoggingUsageProcessingStrategy loggingUsageProcessingStrategy, Tracer tracer) {
        super(new UsageHttpConfiguration(edhHttpConfiguration.isEnabled(), edhHttpConfiguration.getWorkers(), edhHttpConfiguration.getQueueSizeLimit(),
                edhHttpConfiguration.getEndpoint()));
        this.loggingUsageProcessingStrategy = loggingUsageProcessingStrategy;
        this.tracer = tracer;
    }

    @Override
    public UsageHttpRecordWorker createWorker(String threadName, BlockingDeque<UsageHttpRecordRequest> processingQueue) {
        return new UsageHttpRecordWorker(threadName, getServiceName(), this, processingQueue, getConfiguration(), tracer);
    }

    @Override
    public String getServiceName() {
        return "usage-http";
    }

    @Override
    public void handleDroppedRecordRequest(UsageHttpRecordRequest input, int sizeLimit) {
        LOGGER.debug("Usage http record dropped because of queue size limit ({}). Falling back to usage logging.", sizeLimit);
        loggingUsageProcessingStrategy.processUsage(input.messageBodyAsUsageEvent(), null);
    }

    @Override
    public void handleDataStreamingException(UsageHttpRecordRequest input, StreamProcessingException e) {
        LOGGER.debug("Error during data streaming. Falling back to usage logging.", e);
        loggingUsageProcessingStrategy.processUsage(input.messageBodyAsUsageEvent(), null);
    }

    @Override
    public void handleUnexpectedException(UsageHttpRecordRequest input, Exception e) {
        LOGGER.debug("Unexpected error during data streaming. Falling back to usage logging.", e);
        loggingUsageProcessingStrategy.processUsage(input.messageBodyAsUsageEvent(), null);
    }

    @Override
    public boolean isProcessingEnabled() {
        return getConfiguration() != null && getConfiguration().isEnabled() && StringUtils.isNotBlank(getConfiguration().getEndpoint());
    }
}
