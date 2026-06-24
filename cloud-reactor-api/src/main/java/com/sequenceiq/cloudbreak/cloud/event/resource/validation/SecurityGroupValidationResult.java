package com.sequenceiq.cloudbreak.cloud.event.resource.validation;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

/**
 * Result of {@link SecurityGroupValidationRequest}. The two ID sets are always non-null; either being non-empty means
 * validation failed and the caller should surface a diagnostic. When the request did not carry a network ID,
 * {@link #getNotInNetworkSecurityGroupIds()} is always empty regardless of provider state.
 */
public class SecurityGroupValidationResult extends CloudPlatformResult implements FlowPayload {

    private final Set<String> missingSecurityGroupIds;

    private final Set<String> notInNetworkSecurityGroupIds;

    public SecurityGroupValidationResult(Long resourceId, Set<String> missingSecurityGroupIds, Set<String> notInNetworkSecurityGroupIds) {
        super(resourceId);
        this.missingSecurityGroupIds = toMutableSet(missingSecurityGroupIds);
        this.notInNetworkSecurityGroupIds = toMutableSet(notInNetworkSecurityGroupIds);
    }

    public SecurityGroupValidationResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        this.missingSecurityGroupIds = new HashSet<>();
        this.notInNetworkSecurityGroupIds = new HashSet<>();
    }

    @JsonCreator
    public SecurityGroupValidationResult(
            @JsonProperty("status") EventStatus status,
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("missingSecurityGroupIds") Set<String> missingSecurityGroupIds,
            @JsonProperty("notInNetworkSecurityGroupIds") Set<String> notInNetworkSecurityGroupIds) {
        super(status != null ? status : EventStatus.OK, statusReason, errorDetails, resourceId);
        this.missingSecurityGroupIds = toMutableSet(missingSecurityGroupIds);
        this.notInNetworkSecurityGroupIds = toMutableSet(notInNetworkSecurityGroupIds);
    }

    public Set<String> getMissingSecurityGroupIds() {
        return missingSecurityGroupIds;
    }

    public Set<String> getNotInNetworkSecurityGroupIds() {
        return notInNetworkSecurityGroupIds;
    }

    @Override
    public String toString() {
        return "SecurityGroupValidationResult{"
                + "status=" + getStatus()
                + ", statusReason='" + getStatusReason() + '\''
                + ", errorDetails=" + getErrorDetails()
                + ", resourceId=" + getResourceId()
                + ", missingSecurityGroupIds=" + missingSecurityGroupIds
                + ", notInNetworkSecurityGroupIds=" + notInNetworkSecurityGroupIds
                + '}';
    }

    private static Set<String> toMutableSet(Set<String> values) {
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }
}
