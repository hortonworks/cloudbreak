package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class ModifyUserDefinedTagsStackHandlerEvent extends StackEvent {

    private final Map<String, String> userDefinedTags;

    private final Set<String> tagsToRemove;

    public ModifyUserDefinedTagsStackHandlerEvent(Long resourceId, Map<String, String> userDefinedTags) {
        this(resourceId, userDefinedTags, Set.of());
    }

    @JsonCreator
    public ModifyUserDefinedTagsStackHandlerEvent(@JsonProperty("resourceId") Long resourceId,
            @JsonProperty("userDefinedTags") Map<String, String> userDefinedTags,
            @JsonProperty("tagsToRemove") Set<String> tagsToRemove) {
        super(EventSelectorUtil.selector(ModifyUserDefinedTagsStackHandlerEvent.class), resourceId);
        this.userDefinedTags = userDefinedTags;
        this.tagsToRemove = tagsToRemove != null ? tagsToRemove : Set.of();
    }

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    public Set<String> getTagsToRemove() {
        return tagsToRemove;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ModifyUserDefinedTagsStackHandlerEvent.class, other,
                event -> Objects.equals(getResourceId(), event.getResourceId()));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ModifyUserDefinedTagsStackHandlerEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("resourceId=" + getResourceId())
                .add("userDefinedTags=" + userDefinedTags)
                .add("tagsToRemove=" + tagsToRemove)
                .toString();
    }
}
