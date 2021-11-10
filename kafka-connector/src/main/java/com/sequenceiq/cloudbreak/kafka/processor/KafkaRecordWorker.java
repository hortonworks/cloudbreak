package com.sequenceiq.cloudbreak.kafka.processor;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.kafka.config.KafkaConfiguration;
import com.sequenceiq.cloudbreak.kafka.model.KafkaRecordRequest;
import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;
import com.sequenceiq.cloudbreak.streaming.processor.RecordWorker;

import io.opentracing.Tracer;

public class KafkaRecordWorker extends RecordWorker<AbstractKafkaRecordProcessor, KafkaConfiguration, KafkaRecordRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaRecordWorker.class);

    private KafkaProducer<String, String> kafkaProducer;

    private final Tracer tracer;

    public KafkaRecordWorker(String name, String serviceName, AbstractKafkaRecordProcessor recordProcessor,
            BlockingDeque<KafkaRecordRequest> processingQueue, KafkaConfiguration configuration, Tracer tracer) {
        super(name, serviceName, recordProcessor, processingQueue, configuration);
        this.tracer = tracer;
    }

    public KafkaProducer<String, String> getProducer() {
        if (kafkaProducer == null) {
            Properties properties = new Properties();
            KafkaConfiguration kafkaConfiguration = getConfiguration();
            properties.put("bootstrap.servers", Joiner.on(",").join(kafkaConfiguration.getBrokers()));
            properties.put("retries", 1);
            properties.put("key.serializer", StringSerializer.class.getCanonicalName());
            properties.put("value.serializer", StringSerializer.class.getCanonicalName());
            properties.put("acks", "all");
            Properties additionalProperties = kafkaConfiguration.getAdditionalProperties();
            if (additionalProperties != null && !additionalProperties.isEmpty()) {
                additionalProperties.forEach(properties::put);
            }
            kafkaProducer = new KafkaProducer<>(properties);
        }
        return kafkaProducer;
    }

    @Override
    public void processRecordInput(KafkaRecordRequest input) throws StreamProcessingException {
        try {
            ProducerRecord<String, String> newRecord = new ProducerRecord<>(getConfiguration().getTopic(), input.getKey(), getEventMessage(input));
            Future<RecordMetadata> result = getProducer().send(newRecord);
            RecordMetadata recordMetadata = result.get();
            LOGGER.debug("Kafka record is processed with record metadata: {}", recordMetadata);
        } catch (Exception e) {
            throw new StreamProcessingException(e);
        }
    }

    @Override
    public void onInterrupt() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }

    private String getEventMessage(KafkaRecordRequest request) throws StreamProcessingException {
        Optional<GeneratedMessageV3> grpcMessage = request.getMessageBody();
        final String eventMessage;
        if (grpcMessage.isEmpty()) {
            Optional<String> rawMessage = request.getRawBody();
            if (rawMessage.isEmpty()) {
                throw new StreamProcessingException("At least raw body message needs to be filled for kafka input record.");
            } else {
                eventMessage = rawMessage.get();
            }
        } else {
            try {
                eventMessage = JsonFormat.printer()
                        .omittingInsignificantWhitespace().print(grpcMessage.get());
            } catch (InvalidProtocolBufferException e) {
                throw new StreamProcessingException("Error during transforming grpc record to json string", e);
            }
        }
        return eventMessage;
    }

}
