package com.sequenceiq.datalake.events;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;

public class RootVolumeUpdateRequest {

    private final String volumeType;

    private final int size;

    private final String group;

    private final DiskType diskType;

    @JsonCreator
    private RootVolumeUpdateRequest(
            @JsonProperty("volumeType") String volumeType,
            @JsonProperty("size") int size,
            @JsonProperty("group") String group,
            @JsonProperty("diskType") DiskType diskType) {
        this.diskType = diskType;
        this.group = group;
        this.size = size;
        this.volumeType = volumeType;
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

    public DiskType getDiskType() {
        return diskType;
    }

    public static RootVolumeUpdateRequest.Builder builder() {
        return new RootVolumeUpdateRequest.Builder();
    }

    public static RootVolumeUpdateRequest convert(DiskUpdateRequest diskUpdateRequest) {
        return RootVolumeUpdateRequest.builder().withVolumeType(diskUpdateRequest.getVolumeType())
                .withGroup(diskUpdateRequest.getGroup()).withSize(diskUpdateRequest.getSize()).withDiskType(diskUpdateRequest.getDiskType()).build();
    }

    public String toString() {
        return new StringJoiner(", ", RootVolumeUpdateRequest.class.getSimpleName() + "[", "]")
                .add("volumeType=" + volumeType)
                .add("size=" + size)
                .add("group=" + group)
                .add("diskType=" + diskType)
                .toString();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String volumeType;

        private int size;

        private String group;

        private DiskType diskType;

        private Builder() {
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

        public Builder withDiskType(DiskType diskType) {
            this.diskType = diskType;
            return this;
        }

        public RootVolumeUpdateRequest build() {
            return new RootVolumeUpdateRequest(volumeType, size, group, diskType);
        }
    }
}
