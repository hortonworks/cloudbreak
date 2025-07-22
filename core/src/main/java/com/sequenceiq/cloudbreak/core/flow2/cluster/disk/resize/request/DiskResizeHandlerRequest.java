package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DiskResizeHandlerRequest extends StackEvent {

    private final String instanceGroup;

    private final List<Volume> volumesToUpdate;

    private final String volumeType;

    private final int size;

    @JsonCreator
    public DiskResizeHandlerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceGroup") String instanceGroup,
            @JsonProperty("volumeType") String volumeType,
            @JsonProperty("size") int size,
            @JsonProperty("volumesToUpdate") List<Volume> volumesToUpdate) {
        super(selector, stackId);
        this.instanceGroup = instanceGroup;
        this.volumesToUpdate = volumesToUpdate;
        this.volumeType = volumeType;
        this.size = size;
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
        return new StringJoiner(", ", DiskResizeHandlerRequest.class.getSimpleName() + "[", "]")
                .add("instanceGroup=" + instanceGroup)
                .add("volumeType=" + volumeType)
                .add("size=" + size)
                .toString();
    }
}