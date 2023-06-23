package com.sequenceiq.cloudbreak.service.externaldatabase.model;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.DatabaseType;

public class DatabaseStackConfigKey {

    private final CloudPlatform cloudPlatform;

    private final DatabaseType databaseType;

    public DatabaseStackConfigKey(CloudPlatform cloudPlatform) {
        this(cloudPlatform, null);
    }

    public DatabaseStackConfigKey(CloudPlatform cloudPlatform, DatabaseType databaseType) {
        this.cloudPlatform = Objects.requireNonNull(cloudPlatform, "cloudPlatform is null");
        this.databaseType = databaseType;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DatabaseStackConfigKey that = (DatabaseStackConfigKey) o;
        return cloudPlatform == that.cloudPlatform && Objects.equals(databaseType, that.databaseType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cloudPlatform, databaseType);
    }
}
