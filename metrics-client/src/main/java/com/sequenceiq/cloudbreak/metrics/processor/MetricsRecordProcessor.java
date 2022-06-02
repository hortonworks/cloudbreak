package com.sequenceiq.cloudbreak.metrics.processor;

import java.util.concurrent.BlockingDeque;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.streaming.processor.AbstractRecordProcessor;

@Component
public class MetricsRecordProcessor extends AbstractRecordProcessor<MetricsProcessorConfiguration, MetricsRecordRequest, MetricsRecordWorker> {

    public MetricsRecordProcessor(MetricsProcessorConfiguration configuration) {
        super(configuration);
    }

    @Override
    public MetricsRecordWorker createWorker(String threadName, BlockingDeque<MetricsRecordRequest> processingQueue) {
        return new MetricsRecordWorker(threadName, getServiceName(), this, processingQueue, getConfiguration());
    }

    @Override
    public String getServiceName() {
        return "PrometheusMetrics";
    }
}
