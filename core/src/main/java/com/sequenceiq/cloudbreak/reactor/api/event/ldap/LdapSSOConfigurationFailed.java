package com.sequenceiq.cloudbreak.reactor.api.event.ldap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class LdapSSOConfigurationFailed extends StackFailureEvent {
    @JsonCreator
    public LdapSSOConfigurationFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception ex) {
        super(stackId, ex);
    }
}
