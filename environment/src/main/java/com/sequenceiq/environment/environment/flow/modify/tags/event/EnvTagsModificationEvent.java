package com.sequenceiq.environment.environment.flow.modify.tags.event;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvTagsModificationEvent extends BaseNamedFlowEvent {

    private final Map<String, String> userDefinedTags;

    @JsonCreator
    public EnvTagsModificationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("userDefinedTags") Map<String, String> userDefinedTags,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.userDefinedTags = userDefinedTags;
    }

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    public static EnvTagsModificationEvent.Builder builder() {
        return new EnvTagsModificationEvent.Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String selector;

        private Long resourceId;

        private String resourceName;

        private String resourceCrn;

        private Map<String, String> userDefinedTags;

        private Promise<AcceptResult> accepted;

        private Builder() {
        }

        public EnvTagsModificationEvent.Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public EnvTagsModificationEvent.Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public EnvTagsModificationEvent.Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public EnvTagsModificationEvent.Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnvTagsModificationEvent.Builder withUserDefinedTags(Map<String, String> tags) {
            this.userDefinedTags = tags;
            return this;
        }

        public EnvTagsModificationEvent.Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvTagsModificationEvent build() {
            return new EnvTagsModificationEvent(
                    selector,
                    resourceId,
                    resourceName,
                    resourceCrn,
                    userDefinedTags,
                    accepted);
        }
    }
}