package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CoreProviderTemplateUpdateEvent extends StackEvent {

    private final String volumeType;

    private final int size;

    private final String group;

    private final String diskType;

    @JsonCreator
    public CoreProviderTemplateUpdateEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("volumeType") String volumeType,
            @JsonProperty("size") int size,
            @JsonProperty("group") String group,
            @JsonProperty("diskType") String diskType) {
        super(selector, stackId);
        this.volumeType = volumeType;
        this.size = size;
        this.group = group;
        this.diskType = diskType;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreProviderTemplateUpdateEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("stackId=" + getResourceId())
                .add("diskType=" + diskType)
                .add("volumeType=" + volumeType)
                .add("size=" + size)
                .add("group=" + group)
                .toString();
    }
}
