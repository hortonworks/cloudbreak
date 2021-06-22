package com.sequenceiq.cloudbreak.audit.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class ListAuditEvent {

    private final String accountId;

    private final String requestId;

    private final Crn.Service eventSource;

    private final ActorBase actor;

    private final Long fromTimestamp;

    private final Long toTimestamp;

    private final Integer pageSize;

    private final String pageToken;

    private ListAuditEvent(Builder builder) {
        this.accountId = builder.accountId;
        this.requestId = builder.requestId;
        this.eventSource = builder.eventSource;
        this.actor = builder.actor;
        this.fromTimestamp = builder.fromTimestamp;
        this.toTimestamp = builder.toTimestamp;
        this.pageSize = builder.pageSize;
        this.pageToken = builder.pageToken;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getRequestId() {
        return requestId;
    }

    public Crn.Service getEventSource() {
        return eventSource;
    }

    public ActorBase getActor() {
        return actor;
    }

    public Long getFromTimestamp() {
        return fromTimestamp;
    }

    public Long getToTimestamp() {
        return toTimestamp;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public String getPageToken() {
        return pageToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String accountId;

        private String requestId;

        private Crn.Service eventSource;

        private ActorBase actor;

        private Long fromTimestamp;

        private Long toTimestamp;

        private Integer pageSize;

        private String pageToken;

        public Builder accountId(final String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder requestId(final String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder eventSource(final Crn.Service eventSource) {
            this.eventSource = eventSource;
            return this;
        }

        public Builder actor(final ActorBase actor) {
            this.actor = actor;
            return this;
        }

        public Builder fromTimestamp(final Long fromTimestamp) {
            this.fromTimestamp = fromTimestamp;
            return this;
        }

        public Builder toTimestamp(final Long toTimestamp) {
            this.toTimestamp = toTimestamp;
            return this;
        }

        public Builder pageSize(final Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder pageToken(final String pageToken) {
            this.pageToken = pageToken;
            return this;
        }

        public ListAuditEvent build() {
            checkNotNull(actor, "Actor CRN cannot be null.");
            return new ListAuditEvent(this);
        }
    }
}
