package com.sequenceiq.cloudbreak.kafka.model;

import com.google.protobuf.GeneratedMessageV3;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;

public class KafkaRecordRequest extends RecordRequest {

    private final String key;

    private KafkaRecordRequest(Builder builder) {
        super(builder.rawBody, builder.messageBody);
        this.key = builder.key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "KafkaRecordRequest{" +
                "key='" + key + '\'' +
                "} " + super.toString();
    }

    public static class Builder {

        private String key;

        private String rawBody;

        private GeneratedMessageV3 messageBody;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public KafkaRecordRequest build() {
            return new KafkaRecordRequest(this);
        }

        public Builder withKey(String key) {
            this.key = key;
            return this;
        }

        public Builder withRawBody(String rawBody) {
            this.rawBody = rawBody;
            return this;
        }

        public Builder withMessageBody(GeneratedMessageV3 messageBody) {
            this.messageBody = messageBody;
            return this;
        }
    }
}
