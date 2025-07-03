package com.sequenceiq.environment.environment.flow.hybrid.setup.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = EnvironmentCrossRealmTrustSetupEvent.Builder.class)
public class EnvironmentCrossRealmTrustSetupEvent extends BaseNamedFlowEvent {

    private final String fqdn;

    private final String ip;

    private final String realm;

    private final String remoteEnvironmentCrn;

    private final String accountId;

    private final String trustSecret;

    public EnvironmentCrossRealmTrustSetupEvent(
            String selector,
            Long resourceId,
            String accountId,
            Promise<AcceptResult> accepted,
            String resourceName,
            String resourceCrn,
            String fqdn,
            String ip,
            String realm,
            String remoteEnvironmentCrn,
            String trustSecret) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.fqdn = fqdn;
        this.ip = ip;
        this.realm = realm;
        this.accountId = accountId;
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
        this.trustSecret = trustSecret;
    }

    public String getFqdn() {
        return fqdn;
    }

    public String getIp() {
        return ip;
    }

    public String getRealm() {
        return realm;
    }

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public String getTrustSecret() {
        return trustSecret;
    }

    public String getAccountId() {
        return accountId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private String fqdn;

        private String ip;

        private String realm;

        private String remoteEnvironmentCrn;

        private String accountId;

        private String trustSecret;

        private Builder() {
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withFqdn(String fqdn) {
            this.fqdn = fqdn;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withIp(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        public Builder withRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
            this.remoteEnvironmentCrn = remoteEnvironmentCrn;
            return this;
        }

        public Builder withTrustSecret(String trustSecret) {
            this.trustSecret = trustSecret;
            return this;
        }

        public EnvironmentCrossRealmTrustSetupEvent build() {
            return new EnvironmentCrossRealmTrustSetupEvent(
                    selector,
                    resourceId,
                    accountId,
                    accepted,
                    resourceName,
                    resourceCrn,
                    fqdn,
                    ip,
                    realm,
                    remoteEnvironmentCrn,
                    trustSecret);
        }
    }
}
