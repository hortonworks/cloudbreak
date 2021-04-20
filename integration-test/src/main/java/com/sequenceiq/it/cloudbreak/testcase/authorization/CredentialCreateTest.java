package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class CredentialCreateTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create credential request is sent with no region in it",
            then = "a credential should be created")
    public void testCreateCredentialWithAccountAdmin(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        testContext
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
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .whenException(credentialTestClient.get(), ForbiddenException.class, expectedMessage("Doesn't have 'environments/describeCredential'" +
                        " right on credential " + String.format("[\\[]name: %s, crn: crn:cdp:environments:us-west-1:.*:credential:.*[]]\\.",
                        testContext.get(CredentialTestDto.class).getName())).withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with no region in it",
            then = "with a different user with no privilige Unathorized request should be returned")
    public void testCreateCredentialWithZeroRoles(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
        testContext
                .given(CredentialTestDto.class)
                .whenException(credentialTestClient.create(), ForbiddenException.class, expectedMessage("You have no right to perform" +
                        " environments/createCredential in account 460c0d8f-ae8e-4dce-9cd7-2351762eb9ac"))
                .validate();
    }
}