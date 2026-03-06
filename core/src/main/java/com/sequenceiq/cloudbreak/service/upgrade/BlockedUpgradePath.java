package com.sequenceiq.cloudbreak.service.upgrade;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BlockedUpgradePath(
        @JsonProperty("from") String from,
        @JsonProperty("to") String to,
        @JsonProperty("entitlement_override") String entitlementOverride,
        @JsonProperty("internal_account_override") boolean internalAccountOverride) {
}
