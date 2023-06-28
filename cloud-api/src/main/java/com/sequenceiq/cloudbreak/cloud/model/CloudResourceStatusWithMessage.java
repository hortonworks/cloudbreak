package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudResourceStatusWithMessage {

    private List<CloudResourceStatus> resourceStatuses;

    private List<String> messages;

    public CloudResourceStatusWithMessage(Builder builder) {
        this.resourceStatuses = builder.resourceStatuses;
        this.messages = builder.messages;
    }

    public List<CloudResourceStatus> getResourceStatuses() {
        return resourceStatuses;
    }

    public List<String> getMessages() {
        return messages;
    }

    public static class Builder {

        private List<CloudResourceStatus> resourceStatuses = new ArrayList<>();

        List<String> messages = new ArrayList<>();

        public CloudResourceStatusWithMessage.Builder withResourceStatuses(List<CloudResourceStatus> resourceStatuses) {
            this.resourceStatuses = resourceStatuses;
            return this;
        }

        public CloudResourceStatusWithMessage.Builder addResourceStatus(CloudResourceStatus resourceStatus) {
            this.resourceStatuses.add(resourceStatus);
            return this;
        }

        public CloudResourceStatusWithMessage.Builder withMessages(List<String> messages) {
            this.messages = messages;
            return this;
        }

        public CloudResourceStatusWithMessage.Builder addMessages(String message) {
            this.messages.add(message);
            return this;
        }

        public CloudResourceStatusWithMessage build() {
            return new CloudResourceStatusWithMessage(this);
        }
    }

}
