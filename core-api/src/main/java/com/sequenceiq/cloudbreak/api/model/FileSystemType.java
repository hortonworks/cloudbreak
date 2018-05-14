package com.sequenceiq.cloudbreak.api.model;

public enum FileSystemType {

    /**
     * @deprecated Wasb integrated is no longer supported
     */
    @Deprecated
    WASB_INTEGRATED(WasbIntegratedFileSystemConfiguration.class),

    GCS(GcsFileSystemConfiguration.class),

    WASB(WasbFileSystemConfiguration.class),

    ADLS(AdlsFileSystemConfiguration.class),

    S3(S3FileSystemConfiguration.class);

    private final Class<? extends FileSystemConfiguration> clazz;

    FileSystemType(Class<? extends FileSystemConfiguration> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends FileSystemConfiguration> getClazz() {
        return clazz;
    }

    public static FileSystemType fromClass(Class clazz) {
        for (FileSystemType fileSystemType : FileSystemType.values()) {
            if (fileSystemType.clazz.equals(clazz)) {
                return fileSystemType;
            }
        }
        return null;
    }
}
