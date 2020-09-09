package com.sequenceiq.cloudbreak.cloud.gcp.view;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

public class GcpDatabaseServerView {

    @VisibleForTesting
    static final String ENGINE_VERSION = "engineVersion";

    private static final int NUM_MB_IN_GB = 1024;

    private final DatabaseServer databaseServer;

    public GcpDatabaseServerView(DatabaseServer databaseServer) {
        this.databaseServer = databaseServer;
    }

    public Long getAllocatedStorageInMb() {
        return databaseServer.getStorageSize() != null ? databaseServer.getStorageSize() * NUM_MB_IN_GB : null;
    }

    public Long getAllocatedStorageInGb() {
        return databaseServer.getStorageSize() != null ? databaseServer.getStorageSize() : null;
    }

    public String getDbServerName() {
        return databaseServer.getServerId();
    }

    public String getDatabaseType() {
        if (databaseServer.getEngine() == null) {
            return null;
        }
        switch (databaseServer.getEngine()) {
            case POSTGRESQL:
                return "POSTGRES";
            default:
                throw new IllegalStateException("Unsupported Azure Database Server engine " + databaseServer.getEngine());
        }
    }

    public String getDbVersion() {
        return databaseServer.getStringParameter(ENGINE_VERSION);
    }

    public String getDatabaseVersion() {
        return getDatabaseType() + "_" + getDbVersion();
    }

    public String getAdminLoginName() {
        return databaseServer.getRootUserName();
    }

    public String getAdminPassword() {
        return databaseServer.getRootPassword();
    }

    public Integer getPort() {
        return databaseServer.getPort();
    }

    public String getLocation() {
        return databaseServer.getLocation();
    }

}
