package com.sequenceiq.common.model;

import java.util.List;

import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.filesystem.BaseFileSystem;
import com.sequenceiq.common.api.filesystem.EfsFileSystem;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;
import com.sequenceiq.common.api.filesystem.HdfsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;

public enum FileSystemType {

    GCS(GcsFileSystem.class, List.of("gs", "gcs"), "{{{ storageName }}}", ""),

    ADLS(AdlsFileSystem.class, List.of("adl"), "{{{ accountName }}}.azuredatalakestore.net/{{{ storageName }}}", ""),

    ADLS_GEN_2(AdlsGen2FileSystem.class, List.of("abfs", "abfss"), "{{{ storageName }}}.dfs.core.windows.net{{{ subFolder }}}", ".dfs.core.windows.net"),

    S3(S3FileSystem.class, List.of("s3a", "s3", "s3n"), "{{{ storageName }}}", ""),

    EFS(EfsFileSystem.class, List.of("efs"), "{{{ storageName }}}", ""),

    HDFS(HdfsFileSystem.class, List.of("hdfs"), "{{{ storageName }}}", "");

    private final Class<? extends BaseFileSystem> clazz;

    private final List<String> protocols;

    private final String defaultPath;

    private final String postFix;

    FileSystemType(Class<? extends BaseFileSystem> clazz, List<String> protocols, String defaultPath, String postFix) {
        this.clazz = clazz;
        this.protocols = protocols;
        this.defaultPath = defaultPath;
        this.postFix = postFix;
    }

    public Class<? extends BaseFileSystem> getClazz() {
        return clazz;
    }

    public String getProtocol() {
        return protocols.get(0);
    }

    public List<String> getProtocols() {
        return protocols;
    }

    public boolean startsWithProtocol(String path) {
        return path != null && protocols.stream().anyMatch(p -> path.startsWith(p + "://"));
    }

    public String stripProtocol(String path) {
        if (path == null) {
            return null;
        }
        for (String p : protocols) {
            String prefix = p + "://";
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
        }
        return path;
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
