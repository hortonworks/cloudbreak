package com.sequenceiq.cloudbreak.common.service;

import java.util.UUID;

public class TransactionMetricsContext {

    private final String txId;

    private final long start;

    public TransactionMetricsContext(long start) {
        this.txId = UUID.randomUUID().toString();
        this.start = start;
    }

    public String getTxId() {
        return txId;
    }

    public long getStart() {
        return start;
    }
}
