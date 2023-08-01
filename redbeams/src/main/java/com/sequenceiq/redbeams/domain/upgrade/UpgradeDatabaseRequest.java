package com.sequenceiq.redbeams.domain.upgrade;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;

public class UpgradeDatabaseRequest {

    private TargetMajorVersion targetMajorVersion;

    private DatabaseServer migratedDatabaseServer;

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    public void setTargetMajorVersion(TargetMajorVersion targetMajorVersion) {
        this.targetMajorVersion = targetMajorVersion;
    }

    public DatabaseServer getMigratedDatabaseServer() {
        return migratedDatabaseServer;
    }

    public void setMigratedDatabaseServer(DatabaseServer migratedDatabaseServer) {
        this.migratedDatabaseServer = migratedDatabaseServer;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseRequest{" +
                "targetMajorVersion=" + targetMajorVersion +
                ", migratedDatabaseServer=" + migratedDatabaseServer +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpgradeDatabaseRequest that = (UpgradeDatabaseRequest) o;
        if (targetMajorVersion != that.targetMajorVersion) {
            return false;
        }
        return Objects.equals(migratedDatabaseServer, that.migratedDatabaseServer);
    }

    @Override
    public int hashCode() {
        int result = targetMajorVersion.hashCode();
        result = 31 * result + (migratedDatabaseServer != null ? migratedDatabaseServer.hashCode() : 0);
        return result;
    }
}
