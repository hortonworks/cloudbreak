package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvProxyModificationDefaultEvent extends BaseNamedFlowEvent implements ProxyConfigModificationEvent {

    private final String proxyConfigCrn;

    private final String previousProxyConfigCrn;

    @JsonCreator
    public EnvProxyModificationDefaultEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("proxyConfigCrn") String proxyConfigCrn,
            @JsonProperty("previousProxyConfigCrn") String previousProxyConfigCrn,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.proxyConfigCrn = proxyConfigCrn;
        this.previousProxyConfigCrn = previousProxyConfigCrn;
    }

    @Override
    public String getProxyConfigCrn() {
        return proxyConfigCrn;
    }

    @Override
    public String getPreviousProxyConfigCrn() {
        return previousProxyConfigCrn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvProxyModificationDefaultEvent)) {
            return false;
        }
        EnvProxyModificationDefaultEvent that = (EnvProxyModificationDefaultEvent) o;
        return isClassAndEqualsEvent(EnvProxyModificationDefaultEvent.class, that)
                && Objects.equals(proxyConfigCrn, that.proxyConfigCrn)
                && Objects.equals(previousProxyConfigCrn, that.previousProxyConfigCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getResourceId(), proxyConfigCrn, previousProxyConfigCrn);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String proxyConfigCrn;

        private String previousProxyConfigCrn;

        private String selector;

        private Long resourceId;

        private String resourceCrn;

        private Promise<AcceptResult> accepted;

        private String resourceName;

        private Builder() {
        }

        public Builder withProxyConfigCrn(String proxyConfigCrn) {
            this.proxyConfigCrn = proxyConfigCrn;
            return this;
        }

        public Builder withPreviousProxyConfigCrn(String previousProxyConfigCrn) {
            this.previousProxyConfigCrn = previousProxyConfigCrn;
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

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public EnvProxyModificationDefaultEvent build() {
            return new EnvProxyModificationDefaultEvent(selector, resourceId, resourceCrn, resourceName, proxyConfigCrn, previousProxyConfigCrn, accepted);
        }
    }
}
