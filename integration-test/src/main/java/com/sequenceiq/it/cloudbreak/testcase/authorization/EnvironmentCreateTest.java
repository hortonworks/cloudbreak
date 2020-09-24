package com.sequenceiq.it.cloudbreak.testcase.authorization;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.base.RightV4;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.assertion.util.CheckResourceRightFalseAssertion;
import com.sequenceiq.it.cloudbreak.assertion.util.CheckResourceRightTrueAssertion;
import com.sequenceiq.it.cloudbreak.assertion.util.CheckRightFalseAssertion;
import com.sequenceiq.it.cloudbreak.assertion.util.CheckRightTrueAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.mock.freeipa.FreeIpaRouteHandler;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil;

public class EnvironmentCreateTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

    @Inject
    private UtilTestClient utilTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service",
            when = "valid create environment request is sent",
            then = "environment should be created but unauthorized users should not be able to access it")
    public void testCreateEnvironment(TestContext testContext) {
        MockedTestContext mockedTestContext = AuthorizationTestUtil.mockCmForFreeipa(testContext, freeIpaRouteHandler);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)

                // testing unauthorized calls for environment
                .when(environmentTestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform any of these actions: environments/describeEnvironment " +
                                "on crn:cdp:environments:.*").withKey("EnvironmentGetAction"))
                .when(environmentTestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform any of these actions: environments/describeEnvironment " +
                                "on crn:cdp:environments:.*").withKey("EnvironmentGetAction"));
        testFreeipaCreation(testContext, mockedTestContext);
        testContext
                //after assignment describe should work for the environment
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withDatahubCreator()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .withEnvironmentUser()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .when(environmentTestClient.delete())
                .validate();
        testCheckRightUtil(testContext, testContext.given(EnvironmentTestDto.class).getCrn());
    }

    private void testCheckRightUtil(TestContext testContext, String envCrn) {
        AuthorizationTestUtil.testCheckRightUtil(testContext, AuthUserKeys.ENV_CREATOR_A, new CheckRightTrueAssertion(),
                Lists.newArrayList(RightV4.ENV_CREATE), utilTestClient);
        AuthorizationTestUtil.testCheckRightUtil(testContext, AuthUserKeys.ZERO_RIGHTS, new CheckRightFalseAssertion(),
                Lists.newArrayList(RightV4.ENV_CREATE), utilTestClient);

        Map<String, List<RightV4>> resourceRightsToCheckForEnv = Maps.newHashMap();
        resourceRightsToCheckForEnv.put(envCrn, Lists.newArrayList(RightV4.ENV_DELETE, RightV4.ENV_START, RightV4.ENV_STOP));
        Map<String, List<RightV4>> resourceRightsToCheckForDhOnEnv = Maps.newHashMap();
        resourceRightsToCheckForDhOnEnv.put(envCrn, Lists.newArrayList(RightV4.DH_CREATE));
        AuthorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ENV_CREATOR_A, new CheckResourceRightTrueAssertion(),
                resourceRightsToCheckForEnv, utilTestClient);
        AuthorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ENV_CREATOR_A, new CheckResourceRightTrueAssertion(),
                resourceRightsToCheckForDhOnEnv, utilTestClient);
        AuthorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ENV_CREATOR_B, new CheckResourceRightFalseAssertion(),
                resourceRightsToCheckForEnv, utilTestClient);
        AuthorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ENV_CREATOR_B, new CheckResourceRightTrueAssertion(),
                resourceRightsToCheckForDhOnEnv, utilTestClient);
        AuthorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ZERO_RIGHTS, new CheckResourceRightFalseAssertion(),
                resourceRightsToCheckForEnv, utilTestClient);
        AuthorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ZERO_RIGHTS, new CheckResourceRightFalseAssertion(),
                resourceRightsToCheckForDhOnEnv, utilTestClient);
    }

    private void testFreeipaCreation(TestContext testContext, MockedTestContext mockedTestContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        testContext
                //testing authorized freeipa calls for the environment
                .given(FreeIpaTestDto.class)
                .withCatalog(mockedTestContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .when(freeIpaTestClient.describe())
                .when(freeIpaTestClient.stop())
                .await(Status.STOPPED)
                .when(freeIpaTestClient.start())
                .await(Status.AVAILABLE)

                //testing unathorized freeipa calls for the environment
                .when(freeIpaTestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform any of these actions: environments/describeEnvironment on " +
                                "crn:cdp:environments:.*").withKey("FreeIpaDescribeAction"))
                .when(freeIpaTestClient.stop(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform any of these actions: environments/stopEnvironment on " +
                                "crn:cdp:environments:.*").withKey("FreeIpaStopAction"))
                .when(freeIpaTestClient.start(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform any of these actions: environments/startEnvironment on " +
                                "crn:cdp:environments:.*").withKey("FreeIpaStartAction"));
    }

}