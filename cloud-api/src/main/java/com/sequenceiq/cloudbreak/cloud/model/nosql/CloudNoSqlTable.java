package com.sequenceiq.cloudbreak.cloud.model.nosql;

import java.util.Objects;

public class CloudNoSqlTable {

    private String name;

    public CloudNoSqlTable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        CloudNoSqlTable that = (CloudNoSqlTable) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "CloudNoSqlTable{" +
                "name='" + name + '\'' +
                '}';
    }
}
