package com.sequenceiq.cloudbreak.kafka.processor;

import java.util.List;
import java.util.concurrent.BlockingDeque;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.kafka.config.KafkaConfiguration;
import com.sequenceiq.cloudbreak.kafka.model.KafkaRecordRequest;
import com.sequenceiq.cloudbreak.streaming.processor.AbstractRecordProcessor;

import io.opentracing.Tracer;

public abstract class AbstractKafkaRecordProcessor extends AbstractRecordProcessor<KafkaConfiguration, KafkaRecordRequest, KafkaRecordWorker> {

    private final Tracer tracer;

    public AbstractKafkaRecordProcessor(KafkaConfiguration configuration, Tracer tracer) {
        super(configuration);
        this.tracer = tracer;
    }

    @Override
    public KafkaRecordWorker createWorker(String threadName, BlockingDeque<KafkaRecordRequest> processingQueue) {
        return new KafkaRecordWorker(threadName, getServiceName(), this, processingQueue, getConfiguration(), tracer);
    }

    @Override
    public boolean validateConfiguration(KafkaConfiguration configuration) {
        List<String> kafkaBrokers = getConfiguration().getBrokers();
        return StringUtils.isNotBlank(getConfiguration().getTopic()) && CollectionUtils.isNotEmpty(kafkaBrokers)
                && StringUtils.isNoneBlank(kafkaBrokers.toArray(new String[0]));
    }
}
