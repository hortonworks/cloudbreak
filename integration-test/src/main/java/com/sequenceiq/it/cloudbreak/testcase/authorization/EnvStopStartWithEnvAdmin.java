package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentPattern;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.authorization.info.model.RightV4;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.assertion.util.CheckResourceRightFalseAssertion;
import com.sequenceiq.it.cloudbreak.assertion.util.CheckResourceRightTrueAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class EnvStopStartWithEnvAdmin extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private UtilTestClient utilTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Inject
    private AuthorizationTestUtil authorizationTestUtil;

    @Inject
    private ResourceCreator resourceCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        //hacky way to let access to image catalog
        initializeDefaultBlueprints(testContext);
        createDefaultImageCatalog(testContext);
        useRealUmsUser(testContext, AuthUserKeys.ENV_ADMIN_A);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service",
            when = "valid create environment request is sent and then datahub is created",
            then = "environment should be created but unauthorized users should not be able to access it")
    public void testCreateEnvironmentWithDhAndStopWithEnvAdmin(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                // testing unauthorized calls for environment
                .whenException(environmentTestClient.describe(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/describeEnvironment' right on any of the environment[(]s[)] "
                                + environmentPattern(testContext)).withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .whenException(environmentTestClient.describe(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/describeEnvironment' right on any of the environment[(]s[)] "
                                + environmentPattern(testContext)).withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .validate();

        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        EnvironmentTestDto environment = testContext.get(EnvironmentTestDto.class);
        resourceCreator.createNewFreeIpa(testContext, environment);
        createDatalake(testContext);

        testContext
                .given(EnvironmentTestDto.class)
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withDatahubCreator()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_ADMIN_A))
                .given(EnvironmentTestDto.class)
                .given(DistroXTestDto.class)
                .when(distroXClient.create(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .await(STACK_AVAILABLE, RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ACCOUNT_ADMIN)))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_ADMIN_A)))
                .await(EnvironmentStatus.ENV_STOPPED, RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_ADMIN_A)))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.start(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_ADMIN_A)))
                .await(EnvironmentStatus.AVAILABLE, RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_ADMIN_A)))
                .validate();

        testCheckRightUtil(testContext, testContext.given(DistroXTestDto.class).getCrn());
    }

    private void testCheckRightUtil(TestContext testContext, String dhCrn) {
        Map<String, List<RightV4>> resourceRightsToCheck = Maps.newHashMap();
        resourceRightsToCheck.put(dhCrn, Lists.newArrayList(RightV4.DH_DELETE, RightV4.DH_START, RightV4.DH_STOP));
        authorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ENV_CREATOR_A, new CheckResourceRightTrueAssertion(),
                resourceRightsToCheck, utilTestClient);
        authorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ENV_CREATOR_B, new CheckResourceRightTrueAssertion(),
                resourceRightsToCheck, utilTestClient);
        authorizationTestUtil.testCheckResourceRightUtil(testContext, AuthUserKeys.ZERO_RIGHTS, new CheckResourceRightFalseAssertion(),
                resourceRightsToCheck, utilTestClient);
    }
}
