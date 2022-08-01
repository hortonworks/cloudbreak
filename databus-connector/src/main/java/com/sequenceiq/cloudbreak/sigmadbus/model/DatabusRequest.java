package com.sequenceiq.cloudbreak.sigmadbus.model;

import java.util.Optional;

import com.google.protobuf.GeneratedMessageV3;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;

public class DatabusRequest extends RecordRequest {

    private final DatabusRequestContext context;

    private DatabusRequest(Builder builder) {
        super(builder.rawBody, builder.messageBody, 0, false);
        this.context = builder.context;
    }

    public Optional<DatabusRequestContext> getContext() {
        return Optional.ofNullable(context);
    }

    @Override
    public String toString() {
        return "DatabusRequest{" +
                "rawBody='" + getRawBody() + '\'' +
                ", messageBody=" + getMessageBody() +
                ", context=" + context +
                '}';
    }

    public static class Builder {

        private String rawBody;

        private GeneratedMessageV3 messageBody;

        private DatabusRequestContext context;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public DatabusRequest build() {
            return new DatabusRequest(this);
        }

        public Builder withRawBody(String rawBody) {
            this.rawBody = rawBody;
            return this;
        }

        public Builder withMessageBody(GeneratedMessageV3 messageBody) {
            this.messageBody = messageBody;
            return this;
        }

        public Builder withContext(DatabusRequestContext context) {
            this.context = context;
            return this;
        }
    }
}
