package com.sequenceiq.cloudbreak.reactor.api.event.ldap;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class LdapSSOConfigurationRequest extends StackEvent {
    public LdapSSOConfigurationRequest(Long stackId) {
        super(stackId);
    }
}
