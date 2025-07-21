package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CoreRootVolumeUpdateTriggerEvent extends StackEvent {

    private final Map<String, List<String>> updatedNodesMap;

    private final String volumeType;

    private final int size;

    private final String group;

    private final String diskType;

    @JsonCreator
    public CoreRootVolumeUpdateTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("updatedNodesMap") Map<String, List<String>> updatedNodesMap,
            @JsonProperty("volumeType") String volumeType,
            @JsonProperty("size") int size,
            @JsonProperty("group") String group,
            @JsonProperty("diskType") String diskType) {
        super(selector, stackId);
        this.updatedNodesMap = updatedNodesMap;
        this.volumeType = volumeType;
        this.size = size;
        this.group = group;
        this.diskType = diskType;
    }

    public Map<String, List<String>> getUpdatedNodesMap() {
        return updatedNodesMap;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CoreRootVolumeUpdateTriggerEvent that = (CoreRootVolumeUpdateTriggerEvent) o;
        return size == that.size
                && Objects.equals(updatedNodesMap, that.updatedNodesMap)
                && Objects.equals(volumeType, that.volumeType)
                && Objects.equals(group, that.group)
                && Objects.equals(diskType, that.diskType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(updatedNodesMap, volumeType, size, group, diskType);
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(
                CoreRootVolumeUpdateTriggerEvent.class, other, event -> equals(other)
        );
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreRootVolumeUpdateTriggerEvent.class.getSimpleName() + "[", "]")
                .add("updatedNodesMap=" + updatedNodesMap)
                .add("diskType=" + diskType)
                .add("volumeType=" + volumeType)
                .add("size=" + size)
                .add("group=" + group)
                .toString();
    }
}
