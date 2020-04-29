package com.sequenceiq.cloudbreak.audit.model;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class ServiceEventData extends EventData {

    private final String version;

    private final String eventDetails;

    public ServiceEventData(Builder builder) {
        this.version = builder.version;
        this.eventDetails = builder.eventDetails;
    }

    public String getVersion() {
        return version;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ServiceEventData{" +
                "version='" + version + '\'' +
                ", eventDetails='" + eventDetails + '\'' +
                "} " + super.toString();
    }

    public static class Builder {

        private String version;

        private String eventDetails;

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withEventDetails(String eventDetails) {
            this.eventDetails = eventDetails;
            return this;
        }

        public ServiceEventData build() {
            checkArgument(StringUtils.isEmpty(eventDetails) || JsonUtil.isValid(eventDetails),
                    "Service Event Details must be a valid JSON.");
            return new ServiceEventData(this);
        }
    }
}
