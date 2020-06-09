package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseBackupContext extends CommonContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackupContext.class);

    private Long stackId;

    private String backupLocation;

    public DatabaseBackupContext(FlowParameters flowParameters, StackEvent event, String backupLocation) {
        super(flowParameters);
        this.stackId = event.getResourceId();
        this.backupLocation = backupLocation;
    }

    public DatabaseBackupContext(FlowParameters flowParameters, Long stackId, String backupLocation) {
        super(flowParameters);
        this.stackId = stackId;
        this.backupLocation = backupLocation;
    }

    public static DatabaseBackupContext from(FlowParameters flowParameters, StackEvent event, String backupLocation) {
        LOGGER.info("HER DatabaseBackupContext.from");
        return new DatabaseBackupContext(flowParameters, event, backupLocation);
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public void setBackupLocation(String backupLocation) {
        this.backupLocation = backupLocation;
    }
}
