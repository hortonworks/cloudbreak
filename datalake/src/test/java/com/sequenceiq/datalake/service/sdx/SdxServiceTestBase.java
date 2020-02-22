package com.sequenceiq.datalake.service.sdx;

public abstract class SdxServiceTestBase {

    protected static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    protected static final String TEST_ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    protected static final String TEST_ENVIRONMENT_NAME = "someEnvironment";

    protected SdxServiceTestBase() {
    }

    public String getTestUserCrn() {
        return TEST_USER_CRN;
    }

    public String getTestEnvironmentCrn() {
        return TEST_ENVIRONMENT_CRN;
    }

    public String getTestEnvironmentName() {
        return TEST_ENVIRONMENT_NAME;
    }

}
