package com.sequenceiq.cloudbreak.job.diskusage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DiskUsageSyncConfig {

    @Value("${diskusage-sync.intervalminutes:720}")
    private int intervalInMinutes;

    @Value("${diskusage-sync.enabled:true}")
    private boolean enabled;

    @Value("${diskusage-sync.dbdiskthreshold:80}")
    private int dbDiskUsageThresholdPercentage;

    @Value("${diskusage-sync.increment:100}")
    private int diskIncrementSize;

    @Value("${diskusage-sync.maxsize:1000}")
    private int maxDiskSize;

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public boolean isDiskUsageSyncEnabled() {
        return enabled;
    }

    public int getDbDiskUsageThresholdPercentage() {
        return dbDiskUsageThresholdPercentage;
    }

    public int getDiskIncrementSize() {
        return diskIncrementSize;
    }

    public int getMaxDiskSize() {
        return maxDiskSize;
    }
}