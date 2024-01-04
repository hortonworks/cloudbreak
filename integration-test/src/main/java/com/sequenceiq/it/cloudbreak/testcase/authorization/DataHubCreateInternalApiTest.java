package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.who;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class DataHubCreateInternalApiTest extends AbstractIntegrationTest {

    private static final String IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String CM_FOR_DISTRO_X = "cm4dstrx";

    @Inject
    private ResourceCreator resourceCreator;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);
        testContext.as(AuthUserKeys.USER_ACCOUNT_ADMIN);
        testContext.as(AuthUserKeys.USER_ENV_CREATOR_B);
        testContext.as(AuthUserKeys.USER_ENV_CREATOR_A);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "internal recipes, blueprints",
            when = "creates datahub through internal api",
            then = "initiator user can do main operations on it")
    public void createInternalBlueprintsAndRecipesAndDataHubFromThem(TestContext testContext) {
        String accountId = Crn.safeFromString(testContext.getTestUsers().getUserByLabel(AuthUserKeys.USER_ENV_CREATOR_A).getCrn()).getAccountId();

        resourceCreator.createDefaultImageCatalog(testContext);
        RecipeTestDto recipe = resourceCreator.createDefaultRecipeInternal(testContext, accountId);
        BlueprintTestDto blueprint = resourceCreator.createDefaultBlueprintInternal(testContext, accountId,
                commonClusterManagerProperties().getRuntimeVersion());

        recipe.when(recipeTestClient.getV4Internal())
                .validate();

        blueprint.when(blueprintTestClient.getV4Internal())
                .validate();

        resourceCreator.createDefaultCredential(testContext);
        resourceCreator.createDefaultEnvironment(testContext);
        resourceCreator.createDefaultFreeIpa(testContext);
        resourceCreator.createDefaultDataLake(testContext);

        DistroXTestDto distroXTestDto = testContext
                .given(DistroXNetworkTestDto.class)
                .given(DistroXImageTestDto.class)
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(DistroXClouderaManagerTestDto.class)
                .given(DistroXClusterTestDto.class)
                .withBlueprintName(blueprint.getName())
                .withClouderaManager()
                .given(DistroXTestDto.class)
                .withRecipe(recipe.getName())
                .withInitiatorUserCrn(testContext.getTestUsers().getUserByLabel(AuthUserKeys.USER_ENV_CREATOR_A).getCrn());

        distroXTestDto
                .withCluster()
                .withImageSettings(testContext.given(DistroXImageTestDto.class))
                .withNetwork(testContext.given(DistroXNetworkTestDto.class));

        CloudbreakUser accountAdmin = testContext.getTestUsers().getUserByLabel(AuthUserKeys.USER_ACCOUNT_ADMIN);
        CloudbreakUser envCreatorA = testContext.getTestUsers().getUserByLabel(AuthUserKeys.USER_ENV_CREATOR_A);
        CloudbreakUser envCreatorB = testContext.getTestUsers().getUserByLabel(AuthUserKeys.USER_ENV_CREATOR_B);
        resourceCreator.createInternalAndWaitAs(distroXTestDto, accountAdmin);

        distroXTestDto.when(distroXTestClient.get(), who(envCreatorA))
                .validate();
        distroXTestDto.when(distroXTestClient.stop(), who(envCreatorA))
                .await(STACK_STOPPED, who(accountAdmin))
                .validate();
        distroXTestDto.when(distroXTestClient.start(), who(envCreatorA))
                .await(STACK_AVAILABLE, who(accountAdmin))
                .validate();
        testContext.given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();
        distroXTestDto.when(distroXTestClient.get(), who(envCreatorB))
                .validate();
        distroXTestDto.when(distroXTestClient.delete(), who(envCreatorA))
                .await(STACK_DELETED, who(accountAdmin))
                .validate();

        testContext.as(AuthUserKeys.USER_ACCOUNT_ADMIN);
    }
}
