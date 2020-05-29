package com.sequenceiq.it.cloudbreak.testcase.authorization;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.mock.freeipa.FreeIpaRouteHandler;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class EnvironmentCreateTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIPATestClient freeIPATestClient;

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.MGMT_CONSOLE_ADMIN_B);
        useRealUmsUser(testContext, AuthUserKeys.MGMT_CONSOLE_ADMIN_A);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service",
            when = "valid create environment request is sent",
            then = "environment should be created but unauthorized users should not be able to access it")
    public void testCreateEnvironment(TestContext testContext) {
        MockedTestContext mockedTestContext = mockCmForFreeipa(testContext);
        useRealUmsUser(testContext, AuthUserKeys.MGMT_CONSOLE_ADMIN_A);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                // role assignment may not finished, thus we checking status with account admin
                // also this will validate the describe permission check for account admin
                .await(EnvironmentStatus.AVAILABLE, RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ACCOUNT_ADMIN)))

                // testing unauthorized calls for environment
                .when(environmentTestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform environments/describeEnvironment on resource crn:cdp.*")
                                .withKey("EnvironmentGetAction"))
                .when(environmentTestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform environments/describeEnvironment on resource crn:cdp.*")
                                .withKey("EnvironmentGetAction"));
        testFreeipaCreation(testContext, mockedTestContext);
        testContext
                .given(EnvironmentTestDto.class)
                // DL and datahub authz needed for delete also, thus execute delete with account admin for now
                .when(environmentTestClient.delete(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ACCOUNT_ADMIN)))
                .awaitForFlow(RunningParameter.key("EnvironmentDeleteAction"))
                .validate();
    }

    private void testFreeipaCreation(TestContext testContext, MockedTestContext mockedTestContext) {
        testContext
                //testing authorized freeipa calls for the environment
                .given(FreeIPATestDto.class)
                .withCatalog(mockedTestContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                // freeipa calls other api's (like accounttag API) thus we need account admin for now
                .when(freeIPATestClient.create(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ACCOUNT_ADMIN)))
                .await(Status.AVAILABLE, RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_A)))
                .when(freeIPATestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_A)))
                .when(freeIPATestClient.stop(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_A)))
                .await(Status.STOPPED, RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_A)))
                .when(freeIPATestClient.start(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_A)))
                .await(Status.AVAILABLE, RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_A)))

                //testing unathorized freeipa calls for the environment
                .when(freeIPATestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform environments/describeEnvironment on resource crn:cdp.*")
                                .withKey("FreeIPADescribeAction"))
                .when(freeIPATestClient.stop(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform environments/stopEnvironment on resource crn:cdp.*")
                                .withKey("FreeIPAStopAction"))
                .when(freeIPATestClient.start(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform environments/startEnvironment on resource crn:cdp.*")
                                .withKey("FreeIPAStartAction"));
    }

    private MockedTestContext mockCmForFreeipa(TestContext testContext) {
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;
        DynamicRouteStack dynamicRouteStack = mockedTestContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/login_password", (request, response) -> {
            response.cookie("ipa_session", "dummysession");
            return "";
        });
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/json", freeIpaRouteHandler);
        return mockedTestContext;
    }

}