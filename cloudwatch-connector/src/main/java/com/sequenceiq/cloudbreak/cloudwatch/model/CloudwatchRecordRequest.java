package com.sequenceiq.cloudbreak.cloudwatch.model;

import com.google.protobuf.GeneratedMessageV3;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;

public class CloudwatchRecordRequest extends RecordRequest {

    private CloudwatchRecordRequest(Builder builder) {
        super(builder.rawBody, builder.messageBody, builder.timestamp, builder.forceRawOutput);
    }

    @Override
    public String toString() {
        return "CloudwatchRecordRequest{} " + super.toString();
    }

    public static class Builder {

        private String rawBody;

        private GeneratedMessageV3 messageBody;

        private long timestamp;

        private boolean forceRawOutput;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public CloudwatchRecordRequest build() {
            return new CloudwatchRecordRequest(this);
        }

        public Builder withRawBody(String rawBody) {
            this.rawBody = rawBody;
            return this;
        }

        public Builder withMessageBody(GeneratedMessageV3 messageBody) {
            this.messageBody = messageBody;
            return this;
        }

        public Builder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withForceRawOutput(boolean forceRawOutput) {
            this.forceRawOutput = forceRawOutput;
            return this;
        }
    }
}
