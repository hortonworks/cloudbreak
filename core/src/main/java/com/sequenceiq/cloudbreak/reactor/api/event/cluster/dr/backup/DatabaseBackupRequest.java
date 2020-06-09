package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseBackupRequest extends StackEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackupRequest.class);

    private String backupLocation;

    public DatabaseBackupRequest(Long stackId, String backupLocation) {
        super(stackId);
        LOGGER.info("HER creating DatabaseBackupRequest");
        this.backupLocation = backupLocation;
    }

    @Override
    public String selector() {
        return "DatabaseBackupRequest";
    }

    public String getBackupLocation() {
        return backupLocation;
    }
}
