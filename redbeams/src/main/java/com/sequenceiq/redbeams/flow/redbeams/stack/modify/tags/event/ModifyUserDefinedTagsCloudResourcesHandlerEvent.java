package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class ModifyUserDefinedTagsCloudResourcesHandlerEvent extends RedbeamsEvent {

    private final Map<String, String> userDefinedTags;

    private final Set<String> tagsToRemove;

    public ModifyUserDefinedTagsCloudResourcesHandlerEvent(Long resourceId, Map<String, String> userDefinedTags) {
        this(resourceId, userDefinedTags, Set.of());
    }

    @JsonCreator
    public ModifyUserDefinedTagsCloudResourcesHandlerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("userDefinedTags") Map<String, String> userDefinedTags,
            @JsonProperty("tagsToRemove") Set<String> tagsToRemove) {
        super(EventSelectorUtil.selector(ModifyUserDefinedTagsCloudResourcesHandlerEvent.class), resourceId);
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
                "ModifyUserDefinedTagsCloudResourcesHandlerEvent{" +
                "selector='" + getSelector() + '\'' +
                ", resourceId='" + getResourceId() + '\'' +
                ", userDefinedTags='" + userDefinedTags + '\'' +
                ", tagsToRemove='" + tagsToRemove + '\'' +
                '}';
    }
}
