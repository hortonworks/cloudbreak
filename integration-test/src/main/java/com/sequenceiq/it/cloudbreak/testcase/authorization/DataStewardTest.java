package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ENV_DATA_STEWARD;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.datalakePattern;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentDatalakePattern;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentPattern;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class DataStewardTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private ResourceCreator resourceCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);
        testContext.as(AuthUserKeys.ACCOUNT_ADMIN);
        initializeDefaultBlueprints(testContext);
        createDefaultImageCatalog(testContext);
        testContext.as(AuthUserKeys.ENV_CREATOR_A);
        testContext.as(ENV_DATA_STEWARD);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service",
            when = "data steward is assigned to a new environment",
            then = "data steward should access the required endpoints, but nothing else")
    public void testDataStewardRoleRights(TestContext testContext) {
        testContext.as(AuthUserKeys.ENV_CREATOR_A);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        EnvironmentTestDto environment = testContext.get(EnvironmentTestDto.class);
        resourceCreator.createNewFreeIpa(testContext, environment);

        CloudbreakUser envDataSteward = testContext.getTestUsers().getUserByLabel(ENV_DATA_STEWARD);

        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_ADMIN_A))
                .withDataSteward()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_DATA_STEWARD))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe(), RunningParameter.who(envDataSteward))
                .whenException(environmentTestClient.stop(), ForbiddenException.class, expectedMessage(
                        "Doesn't have 'environments/stopEnvironment' right on environment "
                        + environmentPattern(testContext)).withWho(envDataSteward))
                .whenException(environmentTestClient.delete(), ForbiddenException.class, expectedMessage(
                        "Doesn't have 'environments/deleteCdpEnvironment' right on environment "
                        + environmentPattern(testContext)).withWho(envDataSteward))
                .validate();

        testContext.as(AuthUserKeys.ENV_CREATOR_A);
        createDatalake(testContext);

        testContext
                .given(SdxInternalTestDto.class)
                .whenException(sdxTestClient.repairInternal("master"), ForbiddenException.class,
                        expectedMessage("Doesn't have 'datalake/repairDatalake' right on any of the environment[(]s[)] " +
                                environmentDatalakePattern(testContext) + " or on " + datalakePattern(testContext))
                                .withWho(envDataSteward))
                .validate();
    }

}
