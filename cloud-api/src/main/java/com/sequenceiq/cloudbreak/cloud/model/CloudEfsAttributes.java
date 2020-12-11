package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.LifeCycleState;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudEfsAttributes {
    public static final String EFS_TAGKEY_NAME = "Name";

    public static final String EFS_TAGKEY_NAME_VALUE_DEFAULT = "defaultName";

    private String fileSystemId;

    private String creationToken;

    private Boolean deleteOnTermination;

    private Map<String, String> tags;

    private String performanceMode;

    private String throughputMode;

    private Double provisionedThroughputInMibps;

    private Boolean encrypted;

    private String kmsKeyId;

    private LifeCycleState fileState;

    @JsonCreator
    public CloudEfsAttributes(@JsonProperty("creationToken") String creationToken, @JsonProperty("deleteOnTermination") Boolean deleteOnTermination,
            @JsonProperty("tags") Map<String, String> tags, @JsonProperty("performanceMode") String performanceMode,
            @JsonProperty("throughputMode") String throughputMode, @JsonProperty("provisionedThroughputInMibps") Double provisionedThroughputInMibps,
            @JsonProperty("encrypted") Boolean encrypted, @JsonProperty("kmsKeyId") String kmsKeyId) {
        this.creationToken = creationToken;
        this.deleteOnTermination = deleteOnTermination;
        this.tags = tags;
        this.performanceMode = performanceMode;
        this.throughputMode = throughputMode;
        this.provisionedThroughputInMibps = provisionedThroughputInMibps;
        this.encrypted = encrypted;
        this.kmsKeyId = kmsKeyId;

        if (this.tags == null) {
            this.tags = new HashMap<>();
        }

        // we don't know the value untile the response from AWS
        this.fileSystemId = null;
        this.fileState = LifeCycleState.PREPARE;
    }

    public CloudEfsAttributes(CloudEfsAttributes oriAttributes) {
        this(oriAttributes.creationToken, oriAttributes.deleteOnTermination, new HashMap<String, String>(oriAttributes.tags),
                oriAttributes.performanceMode, oriAttributes.throughputMode, oriAttributes.provisionedThroughputInMibps, oriAttributes.encrypted,
                oriAttributes.kmsKeyId);
        setFileSystemId(oriAttributes.fileSystemId);
        setFileState(oriAttributes.fileState);
    }

    public String getName() {
        return tags.getOrDefault(EFS_TAGKEY_NAME, EFS_TAGKEY_NAME_VALUE_DEFAULT);
    }

    public void setName(String efsName) {
        tags.put(EFS_TAGKEY_NAME, efsName);
    }

    public String getCreationToken() {
        return creationToken;
    }

    public void setCreationToken(String creationToken) {
        this.creationToken = creationToken;
    }

    public Boolean getDeleteOnTermination() {
        return deleteOnTermination;
    }

    public void setDeleteOnTermination(Boolean deleteOnTermination) {
        this.deleteOnTermination = deleteOnTermination;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public String getPerformanceMode() {
        return performanceMode;
    }

    public void setPerformanceMode(String performanceMode) {
        this.performanceMode = performanceMode;
    }

    public String getThroughputMode() {
        return throughputMode;
    }

    public void setThroughputMode(String throughputMode) {
        this.throughputMode = throughputMode;
    }

    public Double getProvisionedThroughputInMibps() {
        return provisionedThroughputInMibps;
    }

    public void setProvisionedThroughputInMibps(Double provisionedThroughputInMibps) {
        this.provisionedThroughputInMibps = provisionedThroughputInMibps;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    public String getFileSystemId() {
        return fileSystemId;
    }

    public void setFileSystemId(String fileSystemId) {
        this.fileSystemId = fileSystemId;
    }

    public LifeCycleState getFileState() {
        return fileState;
    }

    public void setFileState(LifeCycleState fileState) {
        this.fileState = fileState;
    }

    public static class Builder {
        private String creationToken;

        private Boolean deleteOnTermination;

        private Map<String, String> tags = new HashMap<>();

        private String performanceMode = "generalPurpose";

        private String throughputMode = "bursting";

        private Double provisionedThroughputInMibps;

        private Boolean encrypted = true;

        private String kmsKeyId;

        public Builder withCreationToken(String creationToken) {
            this.creationToken = creationToken;
            return this;
        }

        public Builder withDeleteOnTermination(Boolean deleteOnTermination) {
            this.deleteOnTermination = deleteOnTermination;
            return this;
        }

        public Builder withName(String efsName) {
            tags.put(EFS_TAGKEY_NAME, efsName);
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            if (tags != null) {
                this.tags.putAll(tags);
            }
            return this;
        }

        public Builder withPerformanceMode(String performanceMode) {
            this.performanceMode = performanceMode;
            return this;
        }

        public Builder withThroughputMode(String throughputMode) {
            this.throughputMode = throughputMode;
            return this;
        }

        public Builder withProvisionedThroughputInMibps(Double provisionedThroughputInMibps) {
            this.provisionedThroughputInMibps = provisionedThroughputInMibps;
            return this;
        }

        public Builder withEncrypted(Boolean encrypted) {
            this.encrypted = encrypted;
            return this;
        }

        public Builder withKmsKeyId(String kmsKeyId) {
            this.kmsKeyId = kmsKeyId;
            return this;
        }

        public CloudEfsAttributes build() {
            return new CloudEfsAttributes(creationToken, deleteOnTermination, tags, performanceMode, throughputMode, provisionedThroughputInMibps,
                    encrypted, kmsKeyId);
        }
    }
}
