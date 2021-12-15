package com.sequenceiq.cloudbreak.cloudwatch.processor;

import java.util.concurrent.BlockingDeque;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloudwatch.config.CloudwatchConfiguration;
import com.sequenceiq.cloudbreak.cloudwatch.model.CloudwatchRecordRequest;
import com.sequenceiq.cloudbreak.streaming.processor.AbstractRecordProcessor;

import io.opentracing.Tracer;

public abstract class AbstractCloudwatchRecordProcessor
        extends AbstractRecordProcessor<CloudwatchConfiguration, CloudwatchRecordRequest, CloudwatchRecordWorker> {

    private final Tracer tracer;

    public AbstractCloudwatchRecordProcessor(CloudwatchConfiguration configuration, Tracer tracer) {
        super(configuration);
        this.tracer = tracer;
    }

    @Override
    public CloudwatchRecordWorker createWorker(String threadName, BlockingDeque<CloudwatchRecordRequest> processingQueue) {
        return new CloudwatchRecordWorker(threadName, getServiceName(), this, processingQueue, getConfiguration(), tracer);
    }

    @Override
    public boolean validateConfiguration(CloudwatchConfiguration configuration) {
        return StringUtils.isNoneEmpty(configuration.getLogGroup(), configuration.getLogStream());
    }
}
