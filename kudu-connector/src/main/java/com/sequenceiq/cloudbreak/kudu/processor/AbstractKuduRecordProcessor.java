package com.sequenceiq.cloudbreak.kudu.processor;

import java.util.concurrent.BlockingDeque;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.kudu.config.KuduConfiguration;
import com.sequenceiq.cloudbreak.kudu.model.KuduRecordRequest;
import com.sequenceiq.cloudbreak.streaming.processor.AbstractRecordProcessor;

import io.opentracing.Tracer;

public abstract class AbstractKuduRecordProcessor extends AbstractRecordProcessor<KuduConfiguration, KuduRecordRequest, KuduRecordWorker> {

    private final Tracer tracer;

    public AbstractKuduRecordProcessor(KuduConfiguration configuration, Tracer tracer) {
        super(configuration);
        this.tracer = tracer;
    }

    @Override
    public KuduRecordWorker createWorker(String threadName, BlockingDeque<KuduRecordRequest> processingQueue) {
        return new KuduRecordWorker(threadName, getServiceName(), this, processingQueue, getConfiguration(), tracer);
    }

    @Override
    public boolean validateConfiguration(KuduConfiguration configuration) {
        return CollectionUtils.isNotEmpty(configuration.getServers()) &&  StringUtils.isNoneBlank(configuration.getServers().toArray(new String[0]));
    }
}
