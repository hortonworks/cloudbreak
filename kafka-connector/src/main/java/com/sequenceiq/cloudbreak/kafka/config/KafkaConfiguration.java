package com.sequenceiq.cloudbreak.kafka.config;

import java.util.List;
import java.util.Properties;

import com.sequenceiq.cloudbreak.streaming.config.AbstractStreamingConfiguration;

public class KafkaConfiguration extends AbstractStreamingConfiguration {

    private static final int DEFAULT_NOT_SET_NUMBER_CONFIG = 0;

    private final List<String> brokers;

    private final String topic;

    private final Properties additionalProperties;

    public KafkaConfiguration(boolean enabled, List<String> brokers, String topic) {
        this(enabled, brokers, topic, null);
    }

    public KafkaConfiguration(boolean enabled, List<String> brokers, String topic, Properties additionalProperties) {
        this(enabled, DEFAULT_NOT_SET_NUMBER_CONFIG, DEFAULT_NOT_SET_NUMBER_CONFIG, brokers, topic, additionalProperties);
    }

    public KafkaConfiguration(boolean enabled, int numberOfWorkers, int queueSizeLimit, List<String> brokers, String topic) {
        this(enabled, numberOfWorkers, queueSizeLimit, brokers, topic, null);
    }

    public KafkaConfiguration(boolean enabled, int numberOfWorkers, int queueSizeLimit, List<String> brokers, String topic, Properties additionalProperties) {
        super(enabled, numberOfWorkers, queueSizeLimit);
        this.brokers = brokers;
        this.topic = topic;
        this.additionalProperties = additionalProperties;
    }

    public List<String> getBrokers() {
        return brokers;
    }

    public String getTopic() {
        return topic;
    }

    public Properties getAdditionalProperties() {
        return additionalProperties;
    }
}
