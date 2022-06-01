package com.sequenceiq.consumption.job.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConsumptionConfig {

    @Value("${storageconsumption.intervalminutes:1}")
    private int intervalInMinutes;

    @Value("${storageconsumption.enabled:true}")
    private boolean storageConsumptionEnabled;

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public boolean isStorageConsumptionEnabled() {
        return storageConsumptionEnabled;
    }

}
