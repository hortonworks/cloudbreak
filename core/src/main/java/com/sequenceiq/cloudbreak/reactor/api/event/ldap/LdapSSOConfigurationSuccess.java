package com.sequenceiq.cloudbreak.reactor.api.event.ldap;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class LdapSSOConfigurationSuccess extends StackEvent {
    public LdapSSOConfigurationSuccess(Long stackId) {
        super(stackId);
    }

    public LdapSSOConfigurationSuccess(String selector, Long stackId) {
        super(selector, stackId);
    }
}
