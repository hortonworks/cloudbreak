package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.datalakePattern;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentDatalakePattern;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentPattern;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class DataStewardTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        initializeDefaultBlueprints(testContext);
        createDefaultImageCatalog(testContext);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.ENV_DATA_STEWARD);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service",
            when = "data steward is assigned to a new environment",
            then = "data steward should access the required endpoints, but nothing else")
    public void testDataStewardRoleRights(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);

        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_ADMIN_A))
                .withDataSteward()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_DATA_STEWARD))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_DATA_STEWARD)))
                .whenException(environmentTestClient.stop(), ForbiddenException.class, expectedMessage(
                        "Doesn't have 'environments/stopEnvironment' right on environment "
                        + environmentPattern(testContext)).withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_DATA_STEWARD)))
                .whenException(environmentTestClient.delete(), ForbiddenException.class, expectedMessage(
                        "Doesn't have 'environments/deleteCdpEnvironment' right on environment "
                        + environmentPattern(testContext)).withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_DATA_STEWARD)))
                .validate();

        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDatalake(testContext);

        testContext
                .given(SdxInternalTestDto.class)
                .whenException(sdxTestClient.repairInternal("master"), ForbiddenException.class,
                        expectedMessage("Doesn't have 'datalake/repairDatalake' right on any of the environment[(]s[)] " +
                                environmentDatalakePattern(testContext) + " or on " + datalakePattern(testContext))
                                .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_DATA_STEWARD)))
                .validate();
    }

}
