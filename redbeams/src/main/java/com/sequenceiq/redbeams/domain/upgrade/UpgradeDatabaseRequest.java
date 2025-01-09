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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UpgradeDatabaseRequest that = (UpgradeDatabaseRequest) o;
        return targetMajorVersion == that.targetMajorVersion &&
                Objects.equals(migratedDatabaseServer, that.migratedDatabaseServer);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(targetMajorVersion);
        result = 31 * result + Objects.hashCode(migratedDatabaseServer);
        return result;
    }
}