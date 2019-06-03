package com.sequenceiq.redbeams;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

public final class TestData {

    private TestData() {
    }

    public static Crn getTestCrn(String resourceType, String resourceId) {
        return getTestCrn("myaccount", resourceType, resourceId);
    }

    public static Crn getTestCrn(String accountId, String resourceType, String resourceId) {
        return Crn.safeFromString(String.format("crn:altus:redbeams:us-west-1:%s:%s:%s", accountId, resourceType, resourceId));
    }
}
