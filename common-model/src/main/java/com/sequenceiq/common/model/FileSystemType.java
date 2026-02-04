package com.sequenceiq.common.model;

import java.util.Set;

import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.filesystem.BaseFileSystem;
import com.sequenceiq.common.api.filesystem.EfsFileSystem;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;
import com.sequenceiq.common.api.filesystem.HdfsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;
import com.sequenceiq.common.api.filesystem.WasbIntegratedFileSystem;

public enum FileSystemType {

    /**
     * @deprecated Wasb integrated is no longer supported
     */
    @Deprecated
    WASB_INTEGRATED(WasbIntegratedFileSystem.class, "wasb", "{{{ storageName }}}@{{{ accountName }}}.blob.core.windows.net", ".blob.core.windows.net"),

    GCS(GcsFileSystem.class, "gs", Set.of("gs", "gcs"), "{{{ storageName }}}", ""),

    WASB(WasbFileSystem.class, "wasb", Set.of("wasb"), "{{{ storageName }}}@{{{ accountName }}}.blob.core.windows.net", ".blob.core.windows.net"),

    ADLS(AdlsFileSystem.class, "adl", Set.of("adl"), "{{{ accountName }}}.azuredatalakestore.net/{{{ storageName }}}", ""),

    ADLS_GEN_2(AdlsGen2FileSystem.class, "abfs", Set.of("abfs", "abfss"), "{{{ storageName }}}.dfs.core.windows.net{{{ subFolder }}}", ".dfs.core.windows.net"),

    S3(S3FileSystem.class, "s3a", Set.of("s3", "s3a", "s3n"), "{{{ storageName }}}", ""),

    EFS(EfsFileSystem.class, "efs", Set.of("efs"), "{{{ storageName }}}", ""),

    HDFS(HdfsFileSystem.class, "hdfs", Set.of("hdfs"), "{{{ storageName }}}", "");

    private final Class<? extends BaseFileSystem> clazz;

    private final String protocol;

    private final Set<String> loggingProtocol;

    private final String defaultPath;

    private final String postFix;

    FileSystemType(Class<? extends BaseFileSystem> clazz, String protocol, Set<String> loggingProtocol, String defaultPath, String postFix) {
        this.clazz = clazz;
        this.protocol = protocol;
        this.defaultPath = defaultPath;
        this.postFix = postFix;
        this.loggingProtocol = loggingProtocol;
    }

    FileSystemType(Class<? extends BaseFileSystem> clazz, String protocol, String defaultPath, String postFix) {
        this(clazz, protocol, Set.of(protocol), defaultPath, postFix);
    }

    public Class<? extends BaseFileSystem> getClazz() {
        return clazz;
    }

    public String getProtocol() {
        return protocol;
    }

    public Set<String> getLoggingProtocol() {
        return loggingProtocol;
    }

    public String getDefaultPath() {
        return defaultPath;
    }

    public String getPostFix() {
        return postFix;
    }

    public static FileSystemType fromClass(Class<?> clazz) {
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

    public boolean isAdlsGen2() {
        return ADLS_GEN_2.equals(this);
    }

    public boolean isS3() {
        return S3.equals(this);
    }

    public boolean isEfs() {
        return EFS.equals(this);
    }

    public boolean isAdls() {
        return ADLS.equals(this);
    }

    public boolean isGcs() {
        return GCS.equals(this);
    }

}
