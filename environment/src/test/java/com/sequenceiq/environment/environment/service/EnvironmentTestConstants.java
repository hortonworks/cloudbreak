package com.sequenceiq.environment.environment.service;

import java.util.UUID;

public class EnvironmentTestConstants {
    public static final String ACCOUNT_ID = "accid";

    public static final String USER = UUID.randomUUID().toString();

    public static final String CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + USER;

    private EnvironmentTestConstants() {
    }
}
