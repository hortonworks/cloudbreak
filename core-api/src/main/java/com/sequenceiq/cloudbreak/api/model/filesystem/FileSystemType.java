package com.sequenceiq.cloudbreak.api.model.filesystem;

public enum FileSystemType {

    /**
     * @deprecated Wasb integrated is no longer supported
     */
    @Deprecated
    WASB_INTEGRATED(WasbIntegratedFileSystem.class, "wasb"),

    GCS(GcsFileSystem.class, "gs"),

    WASB(WasbFileSystem.class, "wasb"),

    ADLS(AdlsFileSystem.class, "adl"),

    S3(S3FileSystem.class, "s3a");

    private final Class<? extends BaseFileSystem> clazz;

    private final String protocol;

    FileSystemType(Class<? extends BaseFileSystem> clazz, String protocol) {
        this.clazz = clazz;
        this.protocol = protocol;
    }

    public Class<? extends BaseFileSystem> getClazz() {
        return clazz;
    }

    public String getProtocol() {
        return protocol;
    }

    public static FileSystemType fromClass(Class clazz) {
        for (FileSystemType fileSystemType : FileSystemType.values()) {
            if (fileSystemType.clazz.equals(clazz)) {
                return fileSystemType;
            }
        }
        return null;
    }

    public boolean isWasb() {
        return WASB.equals(this);
    }

    public boolean isS3() {
        return S3.equals(this);
    }

    public boolean isAdls() {
        return ADLS.equals(this);
    }

    public boolean isGcs() {
        return GCS.equals(this);
    }

}
