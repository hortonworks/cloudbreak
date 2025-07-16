package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_VALIDATE_HANDLER_EVENT;

import jakarta.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AddVolumesValidateEvent extends StackEvent {

    private final String instanceGroup;

    @Nonnull
    private final Long numberOfDisks;

    private final String type;

    private final Long size;

    private final CloudVolumeUsageType cloudVolumeUsageType;

    @JsonCreator
    public AddVolumesValidateEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("numberOfDisks") Long numberOfDisks,
            @JsonProperty("type") String type,
            @JsonProperty("size") Long size,
            @JsonProperty("cloudVolumeUsageType") CloudVolumeUsageType cloudVolumeUsageType,
            @JsonProperty("instanceGroup") String instanceGroup) {
        super(ADD_VOLUMES_VALIDATE_HANDLER_EVENT.event(), stackId);
        this.numberOfDisks = numberOfDisks;
        this.type = type;
        this.size = size;
        this.cloudVolumeUsageType = cloudVolumeUsageType;
        this.instanceGroup = instanceGroup;
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
}
