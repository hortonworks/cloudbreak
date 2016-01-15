package com.sequenceiq.cloudbreak.api.model;

public enum FileSystemType {
    DASH(DashFileSystemConfiguration.class),
    WASB_INTEGRATED(WasbIntegratedFileSystemConfiguration.class),
    GCS(GcsFileSystemConfiguration.class),
    WASB(WasbFileSystemConfiguration.class);

    private Class clazz;

    FileSystemType(Class clazz) {
        this.clazz = clazz;
    }

    public Class getClazz() {
        return clazz;
    }
}
