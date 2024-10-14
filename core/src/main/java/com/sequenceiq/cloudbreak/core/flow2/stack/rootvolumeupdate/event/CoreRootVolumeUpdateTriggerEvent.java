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

    @JsonCreator
    public CoreRootVolumeUpdateTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("updatedNodesMap") Map<String, List<String>> updatedNodesMap) {
        super(selector, stackId);
        this.updatedNodesMap = updatedNodesMap;
    }

    public Map<String, List<String>> getUpdatedNodesMap() {
        return updatedNodesMap;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(CoreRootVolumeUpdateTriggerEvent.class, other,
                event -> Objects.equals(updatedNodesMap, event.updatedNodesMap));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreRootVolumeUpdateTriggerEvent.class.getSimpleName() + "[", "]")
                .add("updatedNodesMap=" + updatedNodesMap)
                .toString();
    }
}
