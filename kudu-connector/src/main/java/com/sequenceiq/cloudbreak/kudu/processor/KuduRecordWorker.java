package com.sequenceiq.cloudbreak.kudu.processor;

import java.util.concurrent.BlockingDeque;

import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.kudu.config.KuduConfiguration;
import com.sequenceiq.cloudbreak.kudu.model.KuduRecordRequest;
import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;
import com.sequenceiq.cloudbreak.streaming.processor.RecordWorker;

import io.opentracing.Tracer;

public class KuduRecordWorker extends RecordWorker<AbstractKuduRecordProcessor, KuduConfiguration, KuduRecordRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KuduRecordWorker.class);

    private KuduClient kuduClient;

    private final Tracer tracer;

    public KuduRecordWorker(String name, String serviceName, AbstractKuduRecordProcessor recordProcessor, BlockingDeque<KuduRecordRequest> processingQueue,
            KuduConfiguration configuration, Tracer tracer) {
        super(name, serviceName, recordProcessor, processingQueue, configuration);
        this.tracer = tracer;
    }

    @Override
    public void processRecordInput(KuduRecordRequest input) throws StreamProcessingException {
        // TODO: getKuduClient() insert record directly
        // String tableName = String.format("%s::%s.%s", input.getType(), input.getDatabase(), input.getTable());
    }

    @Override
    public void onInterrupt() {
        if (kuduClient != null) {
            try {
                kuduClient.close();
            } catch (KuduException e) {
                LOGGER.error("Error during closing kudu client", e);
            }
        }
    }

    public KuduClient getKuduClient() {
        if (kuduClient == null) {
            kuduClient = new KuduClient.KuduClientBuilder(getConfiguration().getServers())
                    .build();
        }
        return kuduClient;
    }
}
