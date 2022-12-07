package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.type.Tunnel;

public class UserDataUpdateRequest extends StackEvent {

    private final Tunnel oldTunnel;

    private final boolean modifyProxyConfig;

    @JsonCreator
    public UserDataUpdateRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonProperty("modifyProxyConfig") boolean modifyProxyConfig) {
        super(selector, stackId);
        this.oldTunnel = oldTunnel;
        this.modifyProxyConfig = modifyProxyConfig;
    }

    public UserDataUpdateRequest(Long stackId, Tunnel oldTunnel, boolean modifyProxyConfig) {
        super(stackId);
        this.oldTunnel = oldTunnel;
        this.modifyProxyConfig = modifyProxyConfig;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public boolean isModifyProxyConfig() {
        return modifyProxyConfig;
    }

    @Override
    public String toString() {
        return "UserDataUpdateRequest{" +
                " oldTunnel=" + oldTunnel +
                " modifyProxyConfig=" + modifyProxyConfig +
                "} " + super.toString();
    }

    public static UserDataUpdateRequestBuilder builder() {
        return new UserDataUpdateRequestBuilder();
    }

    public static final class UserDataUpdateRequestBuilder {
        private String selector;

        private Long stackId;

        private Tunnel oldTunnel;

        private boolean modifyProxyConfig;

        private UserDataUpdateRequestBuilder() {
        }

        public UserDataUpdateRequestBuilder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public UserDataUpdateRequestBuilder withStackId(Long stackId) {
            this.stackId = stackId;
            return this;
        }

        public UserDataUpdateRequestBuilder withOldTunnel(Tunnel oldTunnel) {
            this.oldTunnel = oldTunnel;
            return this;
        }

        public UserDataUpdateRequestBuilder withModifyProxyConfig(boolean modifyProxyConfig) {
            this.modifyProxyConfig = modifyProxyConfig;
            return this;
        }

        public UserDataUpdateRequest build() {
            return new UserDataUpdateRequest(selector, stackId, oldTunnel, modifyProxyConfig);
        }
    }
}
