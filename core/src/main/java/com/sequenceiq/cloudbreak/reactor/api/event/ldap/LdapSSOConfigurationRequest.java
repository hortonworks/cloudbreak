package com.sequenceiq.cloudbreak.reactor.api.event.ldap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class LdapSSOConfigurationRequest extends StackEvent {
    @JsonCreator
    public LdapSSOConfigurationRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
