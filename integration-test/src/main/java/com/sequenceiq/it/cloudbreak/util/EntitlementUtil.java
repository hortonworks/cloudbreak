package com.sequenceiq.it.cloudbreak.util;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class EntitlementUtil {

    private EntitlementUtil() {
    }

    /**
     * Tells whether the acting user's account has the given entitlement, as reported by the Cloudbreak user profile.
     */
    public static boolean isEntitled(TestContext testContext, Entitlement entitlement) {
        return testContext.getCloudbreakClient()
                .getDefaultClient(testContext)
                .userProfileV4Endpoint()
                .get()
                .getEntitlements()
                .contains(entitlement.name());
    }
}
