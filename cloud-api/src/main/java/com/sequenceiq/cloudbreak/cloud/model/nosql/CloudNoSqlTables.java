package com.sequenceiq.cloudbreak.cloud.model.nosql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

public class CloudNoSqlTables {

    private List<CloudNoSqlTable> cloudNoSqlTables = new ArrayList<>();

    public CloudNoSqlTables() {
    }

    public CloudNoSqlTables(@NotNull List<CloudNoSqlTable> cloudNoSqlTables) {
        this.cloudNoSqlTables = cloudNoSqlTables;
    }

    public List<CloudNoSqlTable> getCloudNoSqlTables() {
        return cloudNoSqlTables;
    }

    public void setCloudNoSqlTables(@NotNull List<CloudNoSqlTable> cloudNoSqlTables) {
        this.cloudNoSqlTables = cloudNoSqlTables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        CloudNoSqlTables that = (CloudNoSqlTables) o;
        return Objects.equals(cloudNoSqlTables, that.cloudNoSqlTables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cloudNoSqlTables);
    }
}
