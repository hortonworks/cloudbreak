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
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.assertion.util.CheckResourceRightFalseAssertion;
import com.sequenceiq.it.cloudbreak.assertion.util.CheckResourceRightTrueAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.mock.freeipa.FreeIpaRouteHandler;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil;

public class CreateDhWithDatahubCreator extends AbstractIntegrationTest {
    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

    @Inject
    private UtilTestClient utilTestClient;

    @Override
    protected void setupTest(TestContext testContext) {

        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        //hacky way to let access to image catalog
        initializeDefaultBlueprints(testContext);
        createDefaultImageCatalog(testContext);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service",
            when = "valid create environment request is sent and then datahub is created",
            then = "environment should be created but unauthorized users should not be able to access it")
    public void testCreateEnvironmentWithDh(TestContext testContext) {
        AuthorizationTestUtil.mockCmForFreeipa(testContext, freeIpaRouteHandler);
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
                                "on crn:cdp:environments:.*").withKey("EnvironmentGetAction"))
                .validate();
        testContext
                .given(EnvironmentTestDto.class)
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withDatahubCreator()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .withEnvironmentUser()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(EnvironmentTestDto.class)
                .given(DistroXTestDto.class)
                .when(distroXClient.create(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .awaitForFlow(RunningParameter.key("DistroXCreateAction"))
                .await(STACK_AVAILABLE, RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ACCOUNT_ADMIN)))
                .validate();
        testCheckRightUtil(testContext, testContext.given(DistroXTestDto.class).getCrn());
    }

    private void testCheckRightUtil(TestContext testContext, String dhCrn) {
        Map<String, List<RightV4>> resourceRightsToCheck = Maps.newHashMap();
        resourceRightsToCheck.put(dhCrn, Lists.newArrayList(RightV4.DH_DELETE, RightV4.DH_START, RightV4.DH_STOP));
        AuthorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ENV_CREATOR_A, new CheckResourceRightFalseAssertion(),
                resourceRightsToCheck, utilTestClient);
        AuthorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ENV_CREATOR_B, new CheckResourceRightTrueAssertion(),
                resourceRightsToCheck, utilTestClient);
        AuthorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ZERO_RIGHTS, new CheckResourceRightFalseAssertion(),
                resourceRightsToCheck, utilTestClient);
    }
}
