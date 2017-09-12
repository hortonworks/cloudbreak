package com.sequenceiq.cloudbreak.api.model;

public enum FileSystemType {
    WASB_INTEGRATED(WasbIntegratedFileSystemConfiguration.class),
    GCS(GcsFileSystemConfiguration.class),
    WASB(WasbFileSystemConfiguration.class),
    ADLS(AdlsFileSystemConfiguration.class);

    private final Class<? extends FileSystemConfiguration> clazz;

    FileSystemType(Class<? extends FileSystemConfiguration> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends FileSystemConfiguration> getClazz() {
        return clazz;
    }
}
