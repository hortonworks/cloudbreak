package com.sequenceiq.freeipa.flow.stack.modify.tags.event;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ModifyUserDefinedTagsEvent extends StackEvent {

    private final String operationId;

    private final Map<String, String> userDefinedTags;

    private final Set<String> tagsToRemove;

    public ModifyUserDefinedTagsEvent(String selector, Long resourceId, String operationId, Map<String, String> userDefinedTags) {
        this(selector, resourceId, operationId, userDefinedTags, Set.of());
    }

    @JsonCreator
    public ModifyUserDefinedTagsEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("userDefinedTags") Map<String, String> userDefinedTags,
            @JsonProperty("tagsToRemove") Set<String> tagsToRemove) {
        super(selector, resourceId);
        this.operationId = operationId;
        this.userDefinedTags = userDefinedTags;
        this.tagsToRemove = tagsToRemove != null ? tagsToRemove : Set.of();
    }

    public String getOperationId() {
        return operationId;
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
                ", operationId='" + operationId + '\'' +
                ", userDefinedTags='" + userDefinedTags + '\'' +
                ", tagsToRemove='" + tagsToRemove + '\'' +
                '}';
    }
}
