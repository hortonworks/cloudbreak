package com.sequenceiq.cloudbreak.cloud.aws.view;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

import java.util.List;

public class AwsRdsInstanceView {

    @VisibleForTesting
    static final String BACKUP_RETENTION_PERIOD = "backupRetentionPeriod";

    @VisibleForTesting
    static final String ENGINE_VERSION = "engineVersion";

    private final DatabaseServer databaseServer;

    public AwsRdsInstanceView(DatabaseServer databaseServer) {
        this.databaseServer = databaseServer;
    }

    public Long getAllocatedStorage() {
        return databaseServer.getStorageSize();
    }

    public Integer getBackupRetentionPeriod() {
        return databaseServer.getParameter(BACKUP_RETENTION_PERIOD, Integer.class);
    }

    public String getDBInstanceClass() {
        return databaseServer.getFlavor();
    }

    public String getDBInstanceIdentifier() {
        return databaseServer.getServerId();
    }

    public String getEngine() {
        if (databaseServer.getEngine() == null) {
            return null;
        }
        switch (databaseServer.getEngine()) {
            case POSTGRESQL:
                return "postgres";
            default:
                throw new IllegalStateException("Unsupported RDS engine " + databaseServer.getEngine());
        }
    }

    public String getEngineVersion() {
        return databaseServer.getStringParameter(ENGINE_VERSION);
    }

    public String getMasterUsername() {
        return databaseServer.getRootUserName();
    }

    public String getMasterUserPassword() {
        return databaseServer.getRootPassword();
    }

    public List<String> getVPCSecurityGroups() {
        return databaseServer.getSecurity().getCloudSecurityIds();
    }

}
