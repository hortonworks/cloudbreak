package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.Optional;

public class FileSystemConfigQueryObject {

    private final String clusterName;

    private final String storageName;

    private final String clusterDefinitionText;

    private final String fileSystemType;

    private final Optional<String> accountName;

    private final boolean attachedCluster;

    private final boolean datalakeCluster;

    private FileSystemConfigQueryObject(FileSystemConfigQueryObject.Builder builder) {
        this.storageName = builder.storageName;
        this.clusterName = builder.clusterName;
        this.clusterDefinitionText = builder.clusterDefinitionText;
        this.fileSystemType = builder.fileSystemType;
        this.accountName = builder.accountName;
        this.attachedCluster = builder.attachedCluster;
        this.datalakeCluster = builder.datalakeCluster;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getStorageName() {
        return storageName;
    }

    public String getClusterDefinitionText() {
        return clusterDefinitionText;
    }

    public String getFileSystemType() {
        return fileSystemType;
    }

    public Optional<String> getAccountName() {
        return accountName;
    }

    public boolean isAttachedCluster() {
        return attachedCluster;
    }

    public boolean isDatalakeCluster() {
        return datalakeCluster;
    }

    public static class Builder {

        private String clusterName;

        private String storageName;

        private String clusterDefinitionText;

        private String fileSystemType;

        private Optional<String> accountName = Optional.empty();

        private boolean attachedCluster;

        private boolean datalakeCluster;

        public static Builder builder() {
            return new Builder();
        }

        public Builder withStorageName(String storageName) {
            this.storageName = storageName;
            return this;
        }

        public Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public Builder withClusterDefinitionText(String clusterDefinitionText) {
            this.clusterDefinitionText = clusterDefinitionText;
            return this;
        }

        public Builder withFileSystemType(String fileSystemType) {
            this.fileSystemType = fileSystemType;
            return this;
        }

        public Builder withAccountName(String accountName) {
            this.accountName = Optional.ofNullable(accountName);
            return this;
        }

        public Builder withAttachedCluster(boolean attachedCluster) {
            this.attachedCluster = attachedCluster;
            return this;
        }

        public Builder withDatalakeCluster(boolean datalakeCluster) {
            this.datalakeCluster = datalakeCluster;
            return this;
        }

        public FileSystemConfigQueryObject build() {
            return new FileSystemConfigQueryObject(this);
        }
    }
}
