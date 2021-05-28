package com.sequenceiq.cloudbreak.sigmadbus.model;

import java.util.Optional;

import com.google.protobuf.GeneratedMessageV3;

public class DatabusRequest {

    private final String rawBody;

    private final GeneratedMessageV3 messageBody;

    private final DatabusRequestContext context;

    private DatabusRequest(Builder builder) {
        this.rawBody = builder.rawBody;
        this.messageBody = builder.messageBody;
        this.context = builder.context;
    }

    public Optional<String> getRawBody() {
        return Optional.ofNullable(rawBody);
    }

    public Optional<GeneratedMessageV3> getMessageBody() {
        return Optional.ofNullable(messageBody);
    }

    public Optional<DatabusRequestContext> getContext() {
        return Optional.ofNullable(context);
    }

    @Override
    public String toString() {
        return "DatabusRequest{" +
                "rawBody='" + rawBody + '\'' +
                ", messageBody=" + messageBody +
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
