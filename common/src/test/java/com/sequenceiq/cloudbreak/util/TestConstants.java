package com.sequenceiq.cloudbreak.util;

import java.util.UUID;

public class TestConstants {

    public static final String ACCOUNT_ID = "accid";

    public static final String USER = UUID.randomUUID().toString();

    public static final String CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + USER;

    private TestConstants() {
    }

}
