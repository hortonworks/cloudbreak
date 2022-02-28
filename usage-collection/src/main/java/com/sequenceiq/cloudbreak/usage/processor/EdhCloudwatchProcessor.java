package com.sequenceiq.cloudbreak.usage.processor;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.protobuf.GeneratedMessageV3;
import com.sequenceiq.cloudbreak.cloudwatch.config.CloudwatchConfiguration;
import com.sequenceiq.cloudbreak.cloudwatch.model.CloudwatchRecordRequest;
import com.sequenceiq.cloudbreak.cloudwatch.processor.AbstractCloudwatchRecordProcessor;
import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;
import com.sequenceiq.cloudbreak.usage.strategy.LoggingUsageProcessingStrategy;

import io.opentracing.Tracer;

@Component
public class EdhCloudwatchProcessor extends AbstractCloudwatchRecordProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdhCloudwatchProcessor.class);

    private final LoggingUsageProcessingStrategy loggingUsageProcessorStrategy;

    private final Boolean forceLogging;

    public EdhCloudwatchProcessor(EdhCloudwatchConfiguration configuration, Tracer tracer, LoggingUsageProcessingStrategy loggingUsageProcessorStrategy) {
        super(new CloudwatchConfiguration(configuration.isEnabled(), configuration.getWorkers(), configuration.getQueueSizeLimit(),
                configuration.getLogGroup(), configuration.getLogStream(), configuration.getRegion(), configuration.getMaxRetry()), tracer);
        this.loggingUsageProcessorStrategy = loggingUsageProcessorStrategy;
        this.forceLogging = configuration.isForceLogging();
    }

    @Override
    public String getServiceName() {
        return "cloudwatch-edh";
    }

    @Override
    public void handleDroppedRecordRequest(CloudwatchRecordRequest input, int sizeLimit) {
        super.handleDroppedRecordRequest(input, sizeLimit);
        fallbackToLogs(input);
    }

    @Override
    public void handleDataStreamingException(CloudwatchRecordRequest input, StreamProcessingException e) {
        super.handleDataStreamingException(input, e);
        fallbackToLogs(input);
    }

    @Override
    public void handleUnexpectedException(CloudwatchRecordRequest input, Exception e) {
        super.handleUnexpectedException(input, e);
        fallbackToLogs(input);
    }

    @Override
    public void processRecord(CloudwatchRecordRequest input) {
        if (forceLogging) {
            processEventAsLog(input);
        }
        super.processRecord(input);
    }

    private void fallbackToLogs(CloudwatchRecordRequest input) {
        if (forceLogging) {
            LOGGER.debug("No need for falling back to logs reporting, as logging usage reporter is forced to be used.");
            return;
        }
        processEventAsLog(input);
    }

    private void processEventAsLog(CloudwatchRecordRequest input) {
        Optional<GeneratedMessageV3> messageBodyOpt = input.getMessageBody();
        if (messageBodyOpt.isPresent() && messageBodyOpt.get() instanceof UsageProto.Event) {
            UsageProto.Event eventMessage = (UsageProto.Event) messageBodyOpt.get();
            try {
                loggingUsageProcessorStrategy.processUsage(eventMessage);
            } catch (Exception e) {
                LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", eventMessage, e.getMessage());
            }
        } else {
            LOGGER.warn("Could not log binary format of usage event in Cloudwatch request: {}!", input);
        }
    }

}
