package com.sequenceiq.it.cloudbreak.testcase.authorization;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class CredentialCreateTest extends AbstractIntegrationTest {

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.MGMT_CONSOLE_ADMIN_B);
        useRealUmsUser(testContext, AuthUserKeys.MGMT_CONSOLE_ADMIN_A);
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
        useRealUmsUser(testContext, AuthUserKeys.MGMT_CONSOLE_ADMIN_A);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .when(credentialTestClient.get(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_B)))
                .expect(ForbiddenException.class, RunningParameter.key("CredentialGetAction")
                        .withExpectedMessage("You have no right to perform environments/describeCredential on resource crn:cdp.*"))
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
                .when(credentialTestClient.create(), RunningParameter.key("Unauthorized"))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform " +
                                "environments/createCredential in account 460c0d8f-ae8e-4dce-9cd7-2351762eb9ac")
                                .withKey("Unauthorized"))
                .validate();
    }
}