package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.assertion.util.DeploymentPreferencesTestAssertion.platformEnablementValid;
import static com.sequenceiq.it.cloudbreak.newway.assertion.util.DeploymentPreferencesTestAssertion.supportedExternalDatabasesExists;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.newway.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.util.DeploymentPreferencesTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class DeploymentPreferencesTest extends AbstractIntegrationTest {

    @Inject
    private UtilTestClient utilTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak with MOCK deployment prefernces",
            when = "calling the get deployment preferences endpoint",
            then = "the deployment preferences should be returned")
    public void testGetDeploymentPreferences(MockedTestContext testContext) {
        testContext
                .given(DeploymentPreferencesTestDto.class)
                .when(utilTestClient.deploymentPreferencesV4())
                .then(CommonAssert::responseExists)
                .then(supportedExternalDatabasesExists())
                .then(platformEnablementValid())
                .validate();
    }

}
