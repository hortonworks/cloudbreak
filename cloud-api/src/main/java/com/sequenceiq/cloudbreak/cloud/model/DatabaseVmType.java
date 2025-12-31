package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class DatabaseVmType extends StringType {

    private DatabaseVmType(String vmType) {
        super(vmType);
    }

    public static DatabaseVmType databaseVmType(String vmType) {
        return new DatabaseVmType(vmType);
    }

    @Override
    public String toString() {
        return "DatabaseVmType{"
                + "name=" + getValue()
                + '}';
    }
}
