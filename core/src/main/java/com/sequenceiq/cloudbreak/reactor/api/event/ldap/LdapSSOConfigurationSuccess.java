package com.sequenceiq.cloudbreak.reactor.api.event.ldap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class LdapSSOConfigurationSuccess extends StackEvent {
    public LdapSSOConfigurationSuccess(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public LdapSSOConfigurationSuccess(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }
}
