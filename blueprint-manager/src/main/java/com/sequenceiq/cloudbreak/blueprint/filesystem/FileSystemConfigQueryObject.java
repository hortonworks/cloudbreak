package com.sequenceiq.cloudbreak.blueprint.filesystem;

public class FileSystemConfigQueryObject {

    private final String clusterName;

    private final String storageName;

    private final String blueprintText;

    private final String fileSystemType;

    private FileSystemConfigQueryObject(FileSystemConfigQueryObject.Builder builder) {
        this.storageName = builder.storageName;
        this.clusterName = builder.clusterName;
        this.blueprintText = builder.blueprintText;
        this.fileSystemType = builder.fileSystemType;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getStorageName() {
        return storageName;
    }

    public String getBlueprintText() {
        return blueprintText;
    }

    public String getFileSystemType() {
        return fileSystemType;
    }

    public static class Builder {

        private String clusterName;

        private String storageName;

        private String blueprintText;

        private String fileSystemType;

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

        public Builder withBlueprintText(String blueprintText) {
            this.blueprintText = blueprintText;
            return this;
        }

        public Builder withFileSystemType(String fileSystemType) {
            this.fileSystemType = fileSystemType;
            return this;
        }

        public FileSystemConfigQueryObject build() {
            return new FileSystemConfigQueryObject(this);
        }
    }
}
