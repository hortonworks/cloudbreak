package com.sequenceiq.cloudbreak.reactor.api.event.ldap;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class LdapSSOConfigurationFailed extends StackFailureEvent {
    public LdapSSOConfigurationFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
