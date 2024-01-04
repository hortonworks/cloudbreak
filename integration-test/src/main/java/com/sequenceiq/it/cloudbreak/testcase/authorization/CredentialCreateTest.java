package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ACCOUNT_ADMIN;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ENV_CREATOR_A;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ZERO_RIGHTS;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.config.user.TestUsers;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class CredentialCreateTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);
        testContext.as(AuthUserKeys.ACCOUNT_ADMIN);
        testContext.as(AuthUserKeys.ENV_CREATOR_B);
        testContext.as(ENV_CREATOR_A);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create credential request is sent with no region in it",
            then = "a credential should be created")
    public void testCreateCredentialWithAccountAdmin(TestContext testContext) {
        testContext
                .as(ACCOUNT_ADMIN)
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create credential request is sent",
            then = "a credential should be created, but MgmtConsoleAdminB should not be able to retrieve it")
    public void testCreateCredentialWithManagementConsoleAdmin(TestContext testContext) {
        TestUsers testUsers = testContext.getTestUsers();
        testContext
                .as(ENV_CREATOR_A)
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .whenException(credentialTestClient.get(), ForbiddenException.class, expectedMessage("Doesn't have 'environments/describeCredential'" +
                        " right on credential " + String.format("[\\[]name: %s, crn: crn:cdp:environments:us-west-1:.*:credential:.*[]]\\.",
                        testContext.get(CredentialTestDto.class).getName())).withWho(testUsers.getUserByLabel(AuthUserKeys.ENV_CREATOR_B)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with no region in it",
            then = "with a different user with no privilige Unathorized request should be returned")
    public void testCreateCredentialWithZeroRoles(TestContext testContext) {
        testContext
                .as(ZERO_RIGHTS)
                .given(CredentialTestDto.class)
                .whenException(credentialTestClient.create(), ForbiddenException.class, expectedMessage("You have no right to perform" +
                        " environments/createCredential in account .*"))
                .validate();
    }
}
