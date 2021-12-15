package com.sequenceiq.cloudbreak.streaming.model;

import java.util.Optional;

import com.google.protobuf.GeneratedMessageV3;

public class RecordRequest {

    private final String rawBody;

    private final GeneratedMessageV3 messageBody;

    private final long timestamp;

    public RecordRequest(String rawBody, GeneratedMessageV3 messageBody, long timestamp) {
        this.rawBody = rawBody;
        this.messageBody = messageBody;
        this.timestamp = timestamp;
    }

    public Optional<String> getRawBody() {
        return Optional.ofNullable(rawBody);
    }

    public Optional<GeneratedMessageV3> getMessageBody() {
        return Optional.ofNullable(messageBody);
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "RecordRequest{" +
                "rawBody='" + rawBody + '\'' +
                ", messageBody=" + messageBody +
                ", timestamp=" + timestamp +
                '}';
    }
}
