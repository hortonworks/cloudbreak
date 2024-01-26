package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@JsonDeserialize(builder = DiskResizeRequest.Builder.class)
public class DiskResizeRequest extends StackEvent {

    private final String instanceGroup;

    private final DiskUpdateRequest diskUpdateRequest;

    private final List<Volume> volumesToUpdate;

    public DiskResizeRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("instanceGroup") String instanceGroup,
            @JsonProperty("diskUpdateRequest") DiskUpdateRequest diskUpdateRequest,
            @JsonProperty("volumesToUpdate") List<Volume> volumesToUpdate) {
        super(selector, stackId, null);
        this.instanceGroup = instanceGroup;
        this.diskUpdateRequest = diskUpdateRequest;
        this.volumesToUpdate = volumesToUpdate;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public DiskUpdateRequest getDiskUpdateRequest() {
        return diskUpdateRequest;
    }

    public List<Volume> getVolumesToUpdate() {
        return volumesToUpdate;
    }

    public String toString() {
        return new StringJoiner(", ", DiskResizeRequest.class.getSimpleName() + "[", "]")
                .add("instanceGroup=" + instanceGroup)
                .toString();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private Long stackId;

        private String selector;

        private String instanceGroup;

        private DiskUpdateRequest diskUpdateRequest;

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

        public Builder withDiskUpdateRequest(DiskUpdateRequest diskUpdateRequest) {
            this.diskUpdateRequest = diskUpdateRequest;
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
            return new DiskResizeRequest(selector, stackId, instanceGroup, diskUpdateRequest, volumesToUpdate);
        }
    }
}