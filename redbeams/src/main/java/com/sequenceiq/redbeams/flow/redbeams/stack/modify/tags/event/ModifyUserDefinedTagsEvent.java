package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class ModifyUserDefinedTagsEvent extends RedbeamsEvent {

    private final Map<String, String> userDefinedTags;

    private final Set<String> tagsToRemove;

    public ModifyUserDefinedTagsEvent(String selector, Long resourceId, Map<String, String> userDefinedTags) {
        this(selector, resourceId, userDefinedTags, Set.of());
    }

    @JsonCreator
    public ModifyUserDefinedTagsEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("userDefinedTags") Map<String, String> userDefinedTags,
            @JsonProperty("tagsToRemove") Set<String> tagsToRemove) {
        super(selector, resourceId);
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
    public String toString() {
        return super.toString() + ' ' +
                "ModifyUserDefinedTagsEvent{" +
                "selector='" + getSelector() + '\'' +
                ", resourceId='" + getResourceId() + '\'' +
                ", userDefinedTags='" + userDefinedTags + '\'' +
                ", tagsToRemove='" + tagsToRemove + '\'' +
                '}';
    }
}
