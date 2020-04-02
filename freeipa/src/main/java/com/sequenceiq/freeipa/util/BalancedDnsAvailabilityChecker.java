package com.sequenceiq.freeipa.util;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

public class BalancedDnsAvailabilityChecker {

    // feature supported from 2.20
    private static final Versioned BALANCED_DNS_NAME_AFTER_VERSION = () -> "2.19.0";

    private BalancedDnsAvailabilityChecker() {
    }

    public static boolean isBalancedDnsAvailable(Stack stack) {
        Versioned currentVersion = () -> stack.getAppVersion();
        return new VersionComparator().compare(currentVersion, BALANCED_DNS_NAME_AFTER_VERSION) > 0;
    }

}
