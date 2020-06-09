package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.rx.Promise;

public class DatabaseBackupTriggerEvent extends StackEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackupTriggerEvent.class);

    private final String backupLocation;

    public DatabaseBackupTriggerEvent(String selector, Long stackId, String backupLocation) {
        super(selector, stackId);
        LOGGER.info("HER creating DatabaseBackupTriggerEvent()");
        this.backupLocation = backupLocation;
    }

    public DatabaseBackupTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted, String backupLocation) {
        super(event, resourceId, accepted);
        this.backupLocation = backupLocation;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

}
