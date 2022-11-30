package com.sequenceiq.freeipa.flow.stack.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UserDataUpdateRequest extends StackEvent {
    private final String operationId;

    private final Tunnel oldTunnel;

    private final boolean modifyProxyConfig;

    private final boolean chained;

    private final boolean finalFlow;

    @JsonCreator
    public UserDataUpdateRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonProperty("modifyProxyConfig") boolean modifyProxyConfig,
            @JsonProperty("chained") boolean chained,
            @JsonProperty("finalFlow") boolean finalFlow) {
        super(selector, stackId, accepted);
        this.operationId = operationId;
        this.oldTunnel = oldTunnel;
        this.modifyProxyConfig = modifyProxyConfig;
        this.chained = chained;
        this.finalFlow = finalFlow;
    }

    public String getOperationId() {
        return operationId;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public boolean isModifyProxyConfig() {
        return modifyProxyConfig;
    }

    public boolean isChained() {
        return chained;
    }

    public boolean isFinalFlow() {
        return finalFlow;
    }

    @Override
    public String toString() {
        return "UserDataUpdateRequest{" +
                "operationId='" + operationId + '\'' +
                ", oldTunnel=" + oldTunnel +
                ", modifyProxyConfig=" + modifyProxyConfig +
                ", chained=" + chained +
                ", finalFlow=" + finalFlow +
                "} " + super.toString();
    }

    public static UserDataUpdateRequestBuilder builder() {
        return new UserDataUpdateRequestBuilder();
    }

    public static final class UserDataUpdateRequestBuilder {
        private String selector;

        private Long stackId;

        private Promise<AcceptResult> accepted;

        private String operationId;

        private Tunnel oldTunnel;

        private boolean modifyProxyConfig;

        private boolean chained;

        private boolean finalFlow = true;

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

        public UserDataUpdateRequestBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public UserDataUpdateRequestBuilder withOperationId(String operationId) {
            this.operationId = operationId;
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

        public UserDataUpdateRequestBuilder withChained(boolean chained) {
            this.chained = chained;
            return this;
        }

        public UserDataUpdateRequestBuilder withFinalFlow(boolean finalFlow) {
            this.finalFlow = finalFlow;
            return this;
        }

        public UserDataUpdateRequest build() {
            return new UserDataUpdateRequest(selector, stackId, accepted, operationId, oldTunnel, modifyProxyConfig, chained, finalFlow);
        }
    }
}
