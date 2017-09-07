package com.sequenceiq.cloudbreak.api.model;

public enum FileSystemType {
    WASB_INTEGRATED(WasbIntegratedFileSystemConfiguration.class),
    GCS(GcsFileSystemConfiguration.class),
    WASB(WasbFileSystemConfiguration.class),
    ADLS(AdlsFileSystemConfiguration.class);

    private final Class clazz;

    FileSystemType(Class clazz) {
        this.clazz = clazz;
    }

    public Class getClazz() {
        return clazz;
    }
}
