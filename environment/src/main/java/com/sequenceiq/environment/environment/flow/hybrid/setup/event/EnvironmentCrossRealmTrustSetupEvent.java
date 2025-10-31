package com.sequenceiq.environment.environment.flow.hybrid.setup.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = EnvironmentCrossRealmTrustSetupEvent.Builder.class)
public class EnvironmentCrossRealmTrustSetupEvent extends BaseNamedFlowEvent {

    private final KdcType kdcType;

    private final String kdcFqdn;

    private final String kdcIp;

    private final String kdcRealm;

    private final String dnsIp;

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
            KdcType kdcType,
            String kdcFqdn,
            String kdcIp,
            String kdcRealm,
            String dnsIp,
            String remoteEnvironmentCrn,
            String trustSecret) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.kdcType = kdcType;
        this.kdcFqdn = kdcFqdn;
        this.kdcIp = kdcIp;
        this.kdcRealm = kdcRealm;
        this.dnsIp = dnsIp;
        this.accountId = accountId;
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
        this.trustSecret = trustSecret;
    }

    public KdcType getKdcType() {
        return kdcType;
    }

    public String getKdcFqdn() {
        return kdcFqdn;
    }

    public String getKdcIp() {
        return kdcIp;
    }

    public String getKdcRealm() {
        return kdcRealm;
    }

    public String getDnsIp() {
        return dnsIp;
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

    public Builder toBuilder() {
        return EnvironmentCrossRealmTrustSetupEvent.builder()
                .withSelector(selector())
                .withResourceId(getResourceId())
                .withAccountId(getAccountId())
                .withAccepted(accepted())
                .withResourceName(getResourceName())
                .withResourceCrn(getResourceCrn())
                .withKdcType(kdcType)
                .withKdcFqdn(kdcFqdn)
                .withKdcIp(kdcIp)
                .withKdcRealm(kdcRealm)
                .withDnsIp(dnsIp)
                .withRemoteEnvironmentCrn(remoteEnvironmentCrn)
                .withTrustSecret(trustSecret);
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

        private KdcType kdcType;

        private String kdcFqdn;

        private String kdcIp;

        private String kdcRealm;

        private String dnsIp;

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

        public Builder withKdcType(KdcType kdcType) {
            this.kdcType = kdcType;
            return this;
        }

        public Builder withKdcFqdn(String fqdn) {
            this.kdcFqdn = fqdn;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withKdcIp(String ip) {
            this.kdcIp = ip;
            return this;
        }

        public Builder withKdcRealm(String realm) {
            this.kdcRealm = realm;
            return this;
        }

        public Builder withDnsIp(String ip) {
            this.dnsIp = ip;
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
                    kdcType,
                    kdcFqdn,
                    kdcIp,
                    kdcRealm,
                    dnsIp,
                    remoteEnvironmentCrn,
                    trustSecret);
        }
    }
}
