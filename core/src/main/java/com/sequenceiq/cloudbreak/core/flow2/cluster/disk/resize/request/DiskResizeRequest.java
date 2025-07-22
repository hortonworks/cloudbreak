package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@JsonDeserialize(builder = DiskResizeRequest.Builder.class)
public class DiskResizeRequest extends StackEvent {

    private final String instanceGroup;

    private final String volumeType;

    private final int size;

    private final List<Volume> volumesToUpdate;

    public DiskResizeRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("instanceGroup") String instanceGroup,
            @JsonProperty("volumeType") String volumeType,
            @JsonProperty("size") int size,
            @JsonProperty("volumesToUpdate") List<Volume> volumesToUpdate) {
        super(selector, stackId);
        this.instanceGroup = instanceGroup;
        this.volumeType = volumeType;
        this.size = size;
        this.volumesToUpdate = volumesToUpdate;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public int getSize() {
        return size;
    }

    public List<Volume> getVolumesToUpdate() {
        return volumesToUpdate;
    }

    public String toString() {
        return new StringJoiner(", ", DiskResizeRequest.class.getSimpleName() + "[", "]")
                .add("instanceGroup=" + instanceGroup)
                .add("volumeType=" + volumeType)
                .add("size=" + size)
                .toString();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private Long stackId;

        private String selector;

        private String instanceGroup;

        private String volumeType;

        private int size;

        private List<Volume> volumesToUpdate;

        private Builder() {
        }

        public Builder withStackId(Long stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder withInstanceGroup(String instanceGroup) {
            this.instanceGroup = instanceGroup;
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

        public Builder withVolumesToUpdate(List<Volume> volumesToUpdate) {
            this.volumesToUpdate = volumesToUpdate;
            return this;
        }

        public static Builder builder() {
            return new Builder();
        }

        public DiskResizeRequest build() {
            return new DiskResizeRequest(selector, stackId, instanceGroup, volumeType, size, volumesToUpdate);
        }
    }
}