package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event;

import java.util.Map;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ModifyUserDefinedTagsEvent extends StackEvent {

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
        return new StringJoiner(", ", ModifyUserDefinedTagsEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("stackId=" + getResourceId())
                .add("userDefinedTags=" + userDefinedTags)
                .toString();
    }
}
