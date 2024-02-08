package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DiskResizeHandlerRequest extends StackEvent {

    private final String instanceGroup;

    private final DiskUpdateRequest diskUpdateRequest;

    private final List<Volume> volumesToUpdate;

    @JsonCreator
    public DiskResizeHandlerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceGroup") String instanceGroup,
            @JsonProperty("diskUpdateRequest") DiskUpdateRequest diskUpdateRequest,
            @JsonProperty("volumesToUpdate") List<Volume> volumesToUpdate) {
        super(selector, stackId);
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
        return new StringJoiner(", ", DiskResizeHandlerRequest.class.getSimpleName() + "[", "]")
                .add("instanceGroup=" + instanceGroup)
                .toString();
    }
}