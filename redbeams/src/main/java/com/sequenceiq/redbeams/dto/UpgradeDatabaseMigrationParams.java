package com.sequenceiq.redbeams.dto;

import java.util.Objects;
import java.util.Optional;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;

public class UpgradeDatabaseMigrationParams {

    private Long storageSize;

    private String instanceType;

    private String rootUserName;

    private Json attributes;

    public Long getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(Long storageSize) {
        this.storageSize = storageSize;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getRootUserName() {
        return rootUserName;
    }

    public void setRootUserName(String rootUserName) {
        this.rootUserName = rootUserName;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseMigrationParams{" +
                "storageSize=" + storageSize +
                ", instanceType='" + instanceType + '\'' +
                ", rootUserName='" + rootUserName + '\'' +
                ", attributes=" + attributes +
                '}';
    }

    public static UpgradeDatabaseMigrationParams fromDatabaseServer(DatabaseServer databaseServer) {
        return Optional.ofNullable(databaseServer)
                .map(UpgradeDatabaseMigrationParams::getMigrationParams)
                .orElse(null);
    }

    private static UpgradeDatabaseMigrationParams getMigrationParams(DatabaseServer databaseServer) {
        UpgradeDatabaseMigrationParams params = new UpgradeDatabaseMigrationParams();
        params.setAttributes(databaseServer.getAttributes());
        params.setInstanceType(databaseServer.getInstanceType());
        params.setStorageSize(databaseServer.getStorageSize());
        params.setRootUserName(databaseServer.getRootUserName());
        return params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UpgradeDatabaseMigrationParams that = (UpgradeDatabaseMigrationParams) o;

        if (!Objects.equals(storageSize, that.storageSize)) {
            return false;
        }
        if (!Objects.equals(instanceType, that.instanceType)) {
            return false;
        }
        if (!Objects.equals(rootUserName, that.rootUserName)) {
            return false;
        }
        return Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        int result = storageSize != null ? storageSize.hashCode() : 0;
        result = 31 * result + (instanceType != null ? instanceType.hashCode() : 0);
        result = 31 * result + (rootUserName != null ? rootUserName.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }
}
