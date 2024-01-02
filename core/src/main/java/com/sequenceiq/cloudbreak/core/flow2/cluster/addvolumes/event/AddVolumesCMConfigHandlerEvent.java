package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_CM_CONFIGURATION_HANDLER_EVENT;

import java.util.StringJoiner;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AddVolumesCMConfigHandlerEvent extends StackEvent {

    private final String instanceGroup;

    @Nonnull
    private final Long numberOfDisks;

    private final String type;

    private final Long size;

    private final CloudVolumeUsageType cloudVolumeUsageType;

    @JsonCreator
    public AddVolumesCMConfigHandlerEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceGroup") String instanceGroup,
            @JsonProperty("numberOfDisks") Long numberOfDisks,
            @JsonProperty("type") String type,
            @JsonProperty("size") Long size,
            @JsonProperty("cloudVolumeUsageType") CloudVolumeUsageType cloudVolumeUsageType) {
        super(ADD_VOLUMES_CM_CONFIGURATION_HANDLER_EVENT.event(), stackId);
        this.instanceGroup = instanceGroup;
        this.numberOfDisks = numberOfDisks;
        this.type = type;
        this.size = size;
        this.cloudVolumeUsageType = cloudVolumeUsageType;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    @Nonnull
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
        return new StringJoiner(", ", AddVolumesCMConfigHandlerEvent.class.getSimpleName() + "[", "]")
                .add("instanceGroup=" + instanceGroup)
                .add("Type=" + type)
                .add("Size=" + size)
                .add("NumberOfDisks=" + numberOfDisks)
                .add("CloudVolumeUsageType=" + cloudVolumeUsageType)
                .toString();
    }
}