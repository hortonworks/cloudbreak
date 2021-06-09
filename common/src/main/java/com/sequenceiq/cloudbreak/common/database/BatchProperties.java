package com.sequenceiq.cloudbreak.common.database;

public class BatchProperties {

    private final Integer batchSize;

    private final Boolean orderInserts;

    private final Boolean orderUpdates;

    private final Boolean batchVersionedData;

    public BatchProperties(Integer batchSize, Boolean orderInserts, Boolean orderUpdates, Boolean batchVersionedData) {
        this.batchSize = batchSize;
        this.orderInserts = orderInserts;
        this.orderUpdates = orderUpdates;
        this.batchVersionedData = batchVersionedData;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public Boolean getOrderInserts() {
        return orderInserts;
    }

    public Boolean getOrderUpdates() {
        return orderUpdates;
    }

    public Boolean getBatchVersionedData() {
        return batchVersionedData;
    }
}
