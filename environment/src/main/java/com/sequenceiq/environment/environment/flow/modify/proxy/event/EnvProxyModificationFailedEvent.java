package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnvProxyModificationFailedEvent extends BaseFailedFlowEvent implements ProxyConfigModificationEvent {

    private final String proxyConfigCrn;

    private final String previousProxyConfigCrn;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvProxyModificationFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("proxyConfigCrn") String proxyConfigCrn,
            @JsonProperty("previousProxyConfigCrn") String previousProxyConfigCrn,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus,
            @JsonProperty("exception") Exception exception,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(EnvProxyModificationStateSelectors.FAILED_MODIFY_PROXY_EVENT.selector(), resourceId, accepted, resourceName, resourceCrn, exception);
        this.proxyConfigCrn = proxyConfigCrn;
        this.previousProxyConfigCrn = previousProxyConfigCrn;
        this.environmentStatus = environmentStatus;
    }

    public EnvProxyModificationFailedEvent(ProxyConfigModificationEvent event, Exception exception, EnvironmentStatus environmentStatus) {
        this(event.getResourceId(), event.getResourceCrn(), event.getResourceName(), event.getProxyConfigCrn(), event.getPreviousProxyConfigCrn(),
                environmentStatus, exception, event.accepted());
    }

    @Override
    public String getProxyConfigCrn() {
        return proxyConfigCrn;
    }

    @Override
    public String getPreviousProxyConfigCrn() {
        return previousProxyConfigCrn;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvProxyModificationFailedEvent)) {
            return false;
        }
        EnvProxyModificationFailedEvent that = (EnvProxyModificationFailedEvent) o;
        return isClassAndEqualsEvent(EnvProxyModificationFailedEvent.class, that)
                && Objects.equals(proxyConfigCrn, that.proxyConfigCrn)
                && Objects.equals(previousProxyConfigCrn, that.previousProxyConfigCrn)
                && Objects.equals(getException(), that.getException())
                && Objects.equals(getEnvironmentStatus(), that.getEnvironmentStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getResourceId(), getProxyConfigCrn(), getPreviousProxyConfigCrn(), getEnvironmentStatus(), getException());
    }
}
