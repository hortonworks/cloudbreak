package com.sequenceiq.cloudbreak.util;

import java.util.UUID;

public class TestConstants {

    public static final String ACCOUNT_ID = "accid";

    public static final String USER = UUID.randomUUID().toString();

    public static final String CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + USER;

    public static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    public static final String ENV_NAME = "someAwesomeEnv";

    private TestConstants() {
    }

}
