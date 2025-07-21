package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@JsonDeserialize(builder = DistroXDiskUpdateEvent.Builder.class)
public class DistroXDiskUpdateEvent  extends StackEvent {

    private final String volumeType;

    private final int size;

    private final String group;

    private final String diskType;

    private final Long stackId;

    private final String clusterName;

    private final String accountId;

    private final List<Volume> volumesToBeUpdated;

    private final String cloudPlatform;

    public DistroXDiskUpdateEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("clusterName") String clusterName,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("volumesToBeUpdated") List<Volume> volumesToBeUpdated,
            @JsonProperty("cloudPlatform") String cloudPlatform,
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("volumeType") String volumeType,
            @JsonProperty("size") int size,
            @JsonProperty("group") String group,
            @JsonProperty("diskType") String diskType) {
        super(selector, resourceId);
        this.volumeType = volumeType;
        this.size = size;
        this.group = group;
        this.diskType = diskType;
        this.clusterName = clusterName;
        this.accountId = accountId;
        this.volumesToBeUpdated = volumesToBeUpdated;
        this.cloudPlatform = cloudPlatform;
        this.stackId = stackId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getAccountId() {
        return accountId;
    }

    public List<Volume> getVolumesToBeUpdated() {
        return volumesToBeUpdated;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public Long getStackId() {
        return stackId;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public int getSize() {
        return size;
    }

    public String getGroup() {
        return group;
    }

    public String getDiskType() {
        return diskType;
    }

    public String toString() {
        return new StringJoiner(", ", DistroXDiskUpdateEvent.class.getSimpleName() + "[", "]")
            .add("volumeType=" + volumeType)
            .add("size=" + size)
            .add("group=" + group)
            .add("diskType=" + diskType)
            .add("clusterName=" + clusterName)
            .add("volumesToBeUpdated=" + volumesToBeUpdated)
            .add("cloudPlatform=" + cloudPlatform)
            .toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private String selector;

        private Long resourceId;

        private String clusterName;

        private String accountId;

        private List<Volume> volumesToBeUpdated;

        private String cloudPlatform;

        private Long stackId;

        private String volumeType;

        private int size;

        private String group;

        private String diskType;

        private Builder() {
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withVolumesToBeUpdated(List<Volume> volumesToBeUpdated) {
            this.volumesToBeUpdated = volumesToBeUpdated;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withStackId(Long stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder withVolumeType(String volumeType) {
            this.volumeType = volumeType;
            return this;
        }

        public Builder withSize(int size) {
            this.size = size;
            return this;
        }

        public Builder withGroup(String group) {
            this.group = group;
            return this;
        }

        public Builder withDiskType(String diskType) {
            this.diskType = diskType;
            return this;
        }

        public DistroXDiskUpdateEvent build() {
            return new DistroXDiskUpdateEvent(
                    selector,
                    resourceId,
                    clusterName,
                    accountId,
                    volumesToBeUpdated,
                    cloudPlatform,
                    stackId,
                    volumeType,
                    size,
                    group,
                    diskType);
        }
    }
}
