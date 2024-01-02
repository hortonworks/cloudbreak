package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@JsonDeserialize(builder = AddVolumesRequest.Builder.class)
public class AddVolumesRequest extends StackEvent {

    private final String instanceGroup;

    private final Long numberOfDisks;

    private final String type;

    private final Long size;

    private final CloudVolumeUsageType cloudVolumeUsageType;

    public AddVolumesRequest(
            String selector,
            Long stackId,
            Long numberOfDisks,
            String type,
            Long size,
            CloudVolumeUsageType cloudVolumeUsageType,
            String instanceGroup) {
        super(selector, stackId);
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
        return new StringJoiner(", ", AddVolumesRequest.class.getSimpleName() + "[", "]")
                .add("Type=" + type)
                .add("Size=" + size)
                .add("NumberOfDisks=" + numberOfDisks)
                .add("CloudVolumeUsageType=" + cloudVolumeUsageType)
                .add("instanceGroup=" + instanceGroup)
                .toString();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private Long stackId;

        private String selector;

        private String instanceGroup;

        private Long numberOfDisks;

        private String type;

        private Long size;

        private CloudVolumeUsageType cloudVolumeUsageType;

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

        public Builder withNumberOfDisks(Long numberOfDisks) {
            this.numberOfDisks = numberOfDisks;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withSize(Long size) {
            this.size = size;
            return this;
        }

        public Builder withCloudVolumeUsageType(CloudVolumeUsageType cloudVolumeUsageType) {
            this.cloudVolumeUsageType = cloudVolumeUsageType;
            return this;
        }

        public static Builder builder() {
            return new Builder();
        }

        public AddVolumesRequest build() {
            return new AddVolumesRequest(selector, stackId, numberOfDisks, type, size, cloudVolumeUsageType, instanceGroup);
        }
    }
}