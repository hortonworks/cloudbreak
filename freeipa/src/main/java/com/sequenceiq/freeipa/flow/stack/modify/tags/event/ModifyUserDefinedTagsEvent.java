package com.sequenceiq.freeipa.flow.stack.modify.tags.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ModifyUserDefinedTagsEvent extends StackEvent {

    private final String operationId;

    private final Map<String, String> userDefinedTags;

    @JsonCreator
    public ModifyUserDefinedTagsEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("userDefinedTags") Map<String, String> userDefinedTags) {
        super(selector, resourceId);
        this.operationId = operationId;
        this.userDefinedTags = userDefinedTags;
    }

    public String getOperationId() {
        return operationId;
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
                ", operationId='" + operationId + '\'' +
                ", userDefinedTags='" + userDefinedTags + '\'' +
                '}';
    }
}
