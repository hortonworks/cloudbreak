package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;

public class ChangePrimaryGatewayVerifyReplicationFailed extends ChangePrimaryGatewayFailureEvent {

    public static final String FAILED_PHASE = "Verifying FreeIPA replication convergence";

    public ChangePrimaryGatewayVerifyReplicationFailed(Long stackId, Exception exception) {
        super(stackId, FAILED_PHASE, Set.of(), Map.of(), exception);
    }

    @JsonCreator
    public ChangePrimaryGatewayVerifyReplicationFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedPhase") String failedPhase,
            @JsonProperty("success") Set<String> success,
            @JsonProperty("failureDetails") Map<String, String> failureDetails,
            @JsonProperty("exception") Exception exception) {
        super(stackId, failedPhase, success, failureDetails, exception);
    }
}
