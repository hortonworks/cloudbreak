package com.sequenceiq.cloudbreak.audit.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.util.UuidUtil;

public class AuditEvent {

    private final String id;

    private final String accountId;

    private final String requestId;

    private final String eventName;

    private final String sourceIp;

    private final String eventSource;

    private final ActorBase actor;

    private final EventData eventData;

    public AuditEvent(Builder builder) {
        this.id = builder.id;
        this.accountId = builder.accountId;
        this.requestId = builder.requestId;
        this.eventName = builder.eventName;
        this.sourceIp = builder.sourceIp;
        this.eventSource = builder.eventSource;
        this.actor = builder.actor;
        this.eventData = builder.eventData;
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getEventSource() {
        return eventSource;
    }

    public ActorBase getActor() {
        return actor;
    }

    public EventData getEventData() {
        return eventData;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AuditEvent{" +
                "id='" + id + '\'' +
                ", accountId='" + accountId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", eventName='" + eventName + '\'' +
                ", sourceIp='" + sourceIp + '\'' +
                ", eventSource='" + eventSource + '\'' +
                ", actor=" + actor +
                ", eventData=" + eventData +
                '}';
    }

    public static class Builder {

        private String id;

        private String accountId;

        private String requestId;

        private String eventName;

        private String sourceIp;

        private String eventSource;

        private ActorBase actor;

        private EventData eventData;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder withEventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        public Builder withSourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
            return this;
        }

        public Builder withEventSource(String eventSource) {
            this.eventSource = eventSource;
            return this;
        }

        public Builder withActor(ActorBase actor) {
            this.actor = actor;
            return this;
        }

        public Builder withEventData(EventData eventData) {
            this.eventData = eventData;
            return this;
        }

        public AuditEvent build() {
            checkArgument(StringUtils.isBlank(id) || UuidUtil.isValid(id), "ID must be a valid UUID.");
            checkArgument(StringUtils.isNotBlank(accountId), "Account ID name must be provided.");
            checkArgument(StringUtils.isNotBlank(eventName), "Event name must be provided.");
            checkArgument(Crn.Service.fromString(eventSource) != null, "Event source must be a valid service name as represented in a CRN.");
            checkNotNull(actor);
            return new AuditEvent(this);
        }
    }
}
