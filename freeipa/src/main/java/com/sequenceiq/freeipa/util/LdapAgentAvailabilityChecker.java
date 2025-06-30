package com.sequenceiq.freeipa.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class LdapAgentAvailabilityChecker extends AvailabilityChecker {

    private static final String LDAP_PACKAGE_NAME = "freeipa-ldap-agent";

    private static final Versioned LDAP_PACKAGE_MIN_VERSION_WITH_TLS = () -> "1.1.0.3-b525";

    public boolean isLdapAgentTlsSupportAvailable(Stack stack) {
        return isPackageAvailable(stack, LDAP_PACKAGE_NAME, LDAP_PACKAGE_MIN_VERSION_WITH_TLS)
                && doesAllImageSupport(stack, LDAP_PACKAGE_NAME, LDAP_PACKAGE_MIN_VERSION_WITH_TLS);
    }
}
