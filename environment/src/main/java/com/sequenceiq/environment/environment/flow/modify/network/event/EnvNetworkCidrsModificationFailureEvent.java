package com.sequenceiq.environment.environment.flow.modify.network.event;

import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FAILED_MODIFY_NETWORK_CIDRS_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnvNetworkCidrsModificationFailureEvent extends BaseFailedFlowEvent {

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvNetworkCidrsModificationFailureEvent(
            @JsonProperty("resourceId") Long environmentId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus,
            @JsonProperty("exception") Exception exception) {

        super(FAILED_MODIFY_NETWORK_CIDRS_EVENT.selector(), environmentId, resourceName, resourceCrn, exception);
        this.environmentStatus = environmentStatus;
    }

    @Override
    public String selector() {
        return FAILED_MODIFY_NETWORK_CIDRS_EVENT.event();
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @Override
    public String toString() {
        return "EnvNetworkCidrsModificationFailureEvent{" +
                "environmentStatus='" + environmentStatus +
                '}';
    }
}
