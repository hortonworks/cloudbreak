package com.sequenceiq.cloudbreak.streaming.model;

import java.util.Optional;

import com.google.protobuf.GeneratedMessageV3;

public class RecordRequest {

    private final String rawBody;

    private final GeneratedMessageV3 messageBody;

    private final long timestamp;

    private final boolean forceRawOutput;

    public RecordRequest(String rawBody, GeneratedMessageV3 messageBody, long timestamp, boolean forceRawOutput) {
        this.rawBody = rawBody;
        this.messageBody = messageBody;
        this.timestamp = timestamp;
        this.forceRawOutput = forceRawOutput;
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

    public boolean isForceRawOutput() {
        return forceRawOutput;
    }

    @Override
    public String toString() {
        return "RecordRequest{" +
                "rawBody='" + rawBody + '\'' +
                ", messageBody=" + messageBody +
                ", timestamp=" + timestamp +
                ", forceRawOutput=" + forceRawOutput +
                '}';
    }
}
