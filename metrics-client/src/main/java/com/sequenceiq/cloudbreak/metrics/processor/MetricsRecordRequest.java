package com.sequenceiq.cloudbreak.metrics.processor;

import java.util.Optional;

import com.google.protobuf.GeneratedMessageV3;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;

import prometheus.Remote;

public class MetricsRecordRequest extends RecordRequest {

    private final String accountId;

    private final boolean saas;

    public MetricsRecordRequest(GeneratedMessageV3 messageBody, String accountId, boolean saas) {
        super(null, messageBody, 0L, false);
        this.accountId = accountId;
        this.saas = saas;
    }

    public Remote.WriteRequest getWriteRequest() {
        Optional<GeneratedMessageV3> messageBodyOpt = getMessageBody();
        return (Remote.WriteRequest) messageBodyOpt.orElse(null);
    }

    public String getAccountId() {
        return accountId;
    }

    public boolean isSaas() {
        return saas;
    }
}
