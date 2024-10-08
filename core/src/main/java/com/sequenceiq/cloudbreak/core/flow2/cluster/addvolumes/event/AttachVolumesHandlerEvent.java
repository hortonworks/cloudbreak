package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AttachVolumesHandlerEvent extends StackEvent {

    private final String instanceGroup;

    private final Long numberOfDisks;

    private final String type;

    private final Long size;

    private final CloudVolumeUsageType cloudVolumeUsageType;

    @JsonCreator
    public AttachVolumesHandlerEvent(@JsonProperty("resourceId") Long resourceId,
            @JsonProperty("numberOfDisks") Long numberOfDisks,
            @JsonProperty("type") String type,
            @JsonProperty("size") Long size,
            @JsonProperty("cloudVolumeUsageType") CloudVolumeUsageType cloudVolumeUsageType,
            @JsonProperty("instanceGroup") String instanceGroup) {
        super(AddVolumesEvent.ATTACH_VOLUMES_HANDLER_EVENT.event(), resourceId);
        this.numberOfDisks = numberOfDisks;
        this.type = type;
        this.size = size;
        this.cloudVolumeUsageType = cloudVolumeUsageType;
        this.instanceGroup = instanceGroup;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public Long getNumberOfDisks() {
        return numberOfDisks;
    }

    public String getType() {
        return type;
    }

    public Long getSize() {
        return size;
    }

    public CloudVolumeUsageType getCloudVolumeUsageType() {
        return cloudVolumeUsageType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AttachVolumesHandlerEvent.class.getSimpleName() + "[", "]")
                .add("Type=" + type)
                .add("Size=" + size)
                .add("NumberOfDisks=" + numberOfDisks)
                .add("CloudVolumeUsageType=" + cloudVolumeUsageType)
                .add("instanceGroup=" + instanceGroup)
                .toString();
    }
}