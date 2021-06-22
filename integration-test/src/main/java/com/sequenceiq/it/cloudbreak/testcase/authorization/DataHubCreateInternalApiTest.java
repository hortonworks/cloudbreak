package com.sequenceiq.it.cloudbreak.testcase.authorization;

import java.util.Optional;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
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
    private CloudbreakActor cloudbreakActor;

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
        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "internal recipes, blueprints",
            when = "creates datahub through internal api",
            then = "initiator user can do main operations on it")
    public void createInternalBlueprintsAndRecipesAndDataHubFromThem(TestContext testContext) {
        String accountId = Crn.safeFromString(cloudbreakActor.useRealUmsUser(AuthUserKeys.USER_ENV_CREATOR_A).getCrn()).getAccountId();

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
                .withInitiatorUserCrn(cloudbreakActor.useRealUmsUser(AuthUserKeys.USER_ENV_CREATOR_A).getCrn());

        distroXTestDto
                .withCluster()
                .withImageSettings(testContext.given(DistroXImageTestDto.class))
                .withNetwork(testContext.given(DistroXNetworkTestDto.class));

        resourceCreator.createInternalAndWaitAs(distroXTestDto, Optional.of(AuthUserKeys.USER_ACCOUNT_ADMIN));

        RunningParameter envCreatorA = user(AuthUserKeys.USER_ENV_CREATOR_A);
        RunningParameter envCreatorB = user(AuthUserKeys.USER_ENV_CREATOR_B);
        RunningParameter accountAdmin = user(AuthUserKeys.USER_ACCOUNT_ADMIN);
        distroXTestDto.when(distroXTestClient.get(), envCreatorA)
                .validate();
        distroXTestDto.when(distroXTestClient.stop(), envCreatorA)
                .await(STACK_STOPPED, accountAdmin)
                .validate();
        distroXTestDto.when(distroXTestClient.start(), envCreatorA)
                .await(STACK_AVAILABLE, accountAdmin)
                .validate();
        testContext.given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();
        distroXTestDto.when(distroXTestClient.get(), envCreatorB)
                .validate();
        distroXTestDto.when(distroXTestClient.delete(), envCreatorA)
                .await(STACK_DELETED, accountAdmin)
                .validate();

        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
    }

    private RunningParameter user(String userKey) {
        return RunningParameter.who(cloudbreakActor.useRealUmsUser(userKey));
    }
}
