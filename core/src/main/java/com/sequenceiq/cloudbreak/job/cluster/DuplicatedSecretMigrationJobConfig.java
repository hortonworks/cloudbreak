package com.sequenceiq.cloudbreak.job.cluster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DuplicatedSecretMigrationJobConfig {

    @Value("${duplicatedsecretmigration.intervalminutes:10}")
    private int intervalInMinutes;

    @Value("${duplicatedsecretmigration.enabled:true}")
    private boolean duplicatedSecretMigrationEnabled;

    public boolean isDuplicatedSecretMigrationEnabled() {
        return duplicatedSecretMigrationEnabled;
    }

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }
}
