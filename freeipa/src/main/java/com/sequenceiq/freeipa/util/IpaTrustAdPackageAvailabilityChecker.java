package com.sequenceiq.freeipa.util;

import org.springframework.stereotype.Component;

@Component
public class IpaTrustAdPackageAvailabilityChecker extends AvailabilityChecker {

    public static final String IPA_SERVER_TRUST_AD_PACKAGE = "ipa-server-trust-ad";

    public boolean isPackageAvailable(Long stackId) {
        return super.isPackageAvailable(stackId, IPA_SERVER_TRUST_AD_PACKAGE, () -> "0.0.0");
    }
}
