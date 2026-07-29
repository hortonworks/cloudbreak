package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class DatabaseVmType extends StringType {

    private DatabaseVmTypeMeta metaData;

    private DatabaseVmType(String vmType, DatabaseVmTypeMeta meta) {
        super(vmType);
        this.metaData = meta;
    }

    public DatabaseVmTypeMeta getMetaData() {
        return metaData;
    }

    public static DatabaseVmType databaseVmType(String vmType, DatabaseVmTypeMeta meta) {
        return new DatabaseVmType(vmType, meta);
    }

    @Override
    public String toString() {
        return "DatabaseVmType{"
                + "name=" + getValue()
                + ", metaData=" + metaData
                + '}';
    }
}
