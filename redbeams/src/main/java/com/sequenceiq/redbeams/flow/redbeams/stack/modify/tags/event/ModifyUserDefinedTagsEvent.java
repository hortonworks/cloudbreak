package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class ModifyUserDefinedTagsEvent extends RedbeamsEvent {

    private final Map<String, String> userDefinedTags;

    @JsonCreator
    public ModifyUserDefinedTagsEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("userDefinedTags") Map<String, String> userDefinedTags) {
        super(selector, resourceId);
        this.userDefinedTags = userDefinedTags;
    }

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    @Override
    public String toString() {
        return super.toString() + ' ' +
                "ModifyUserDefinedTagsEvent{" +
                "selector='" + getSelector() + '\'' +
                ", resourceId='" + getResourceId() + '\'' +
                ", userDefinedTags='" + userDefinedTags + '\'' +
                '}';
    }
}
