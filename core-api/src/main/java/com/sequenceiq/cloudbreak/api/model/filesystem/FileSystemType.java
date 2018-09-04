package com.sequenceiq.cloudbreak.api.model.filesystem;

public enum FileSystemType {

    /**
     * @deprecated Wasb integrated is no longer supported
     */
    @Deprecated
    WASB_INTEGRATED(WasbIntegratedFileSystem.class, "wasb", "{{{ storageName }}}@{{{ accountName }}}.blob.core.windows.net"),

    GCS(GcsFileSystem.class, "gs", "{{{ storageName }}}/{{{ clusterName }}}"),

    WASB(WasbFileSystem.class, "wasb", "{{{ storageName }}}@{{{ accountName }}}.blob.core.windows.net"),

    ADLS(AdlsFileSystem.class, "adl", "{{{ accountName }}}.azuredatalakestore.net/{{{ storageName }}}"),

    ABFS(AbfsFileSystem.class, "abfs", "{{{ storageName }}}@{{{ accountName }}}.blob.core.windows.net"),

    S3(S3FileSystem.class, "s3a", "{{{ storageName }}}/{{{ clusterName }}}");

    private final Class<? extends BaseFileSystem> clazz;

    private final String protocol;

    private final String defaultPath;

    FileSystemType(Class<? extends BaseFileSystem> clazz, String protocol, String defaultPath) {
        this.clazz = clazz;
        this.protocol = protocol;
        this.defaultPath = defaultPath;
    }

    public Class<? extends BaseFileSystem> getClazz() {
        return clazz;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getDefaultPath() {
        return defaultPath;
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

    public boolean isAbfs() {
        return ABFS.equals(this);
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
