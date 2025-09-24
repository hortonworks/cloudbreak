package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.pollingInterval;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.util.cleanup.ParcelGeneratorUtil;
import com.sequenceiq.it.util.cleanup.ParcelMockActivatorUtil;

public class RecipeClusterTest extends AbstractMockTest {

    private static final int NODE_COUNT = 3;

    private static final String INSTANCE_GROUP_ID = "ig";

    private static final String HIGHSTATE = "state.highstate";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeClusterTest.class);

    private static final String RECIPE_CONTENT = Base64.encodeBase64String("#!/bin/bash\necho ALMAA".getBytes());

    private static final Duration POLLING_INTERVAL = Duration.of(3000, ChronoUnit.MILLIS);

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Inject
    private ParcelMockActivatorUtil parcelMockActivatorUtil;

    @Inject
    private ParcelGeneratorUtil parcelGeneratorUtil;

    @Inject
    private RecipeUtil recipeUtil;

    @Test(dataProvider = "dataProviderForNonPreTerminationRecipeTypes")
    public void testRecipeNotPreTerminationHasGotHighStateOnCluster(
            TestContext testContext,
            RecipeV4Type type,
            int executionTime,
            @Description TestCaseDescription testCaseDescription) {
        LOGGER.info("testing recipe execution for type: {}", type.name());
        String recipeName = resourcePropertyProvider().getName();
        String stackName = resourcePropertyProvider().getName();
        String instanceGroupName = resourcePropertyProvider().getName();

        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(recipeName, RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generateRecipeContent(RECIPE_CONTENT))
                    .withRecipeType(type)
                .when(recipeTestClient.createV4(), RunningParameter.key(recipeName))
                .given(stackName, DistroXTestDto.class)
                .withRecipe(recipeName)
                .when(distroXTestClient.create(), RunningParameter.key(stackName))
                .enableVerification()
                .await(STACK_AVAILABLE, RunningParameter.key(stackName))
                .mockSalt().run().post().bodyContains(HIGHSTATE, 1).atLeast(executionTime).verify()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a deleted recipe",
            when = "starting cluster with deleted recipe",
            then = "badrequest exception is received")
    public void testDeletedRecipeCannotBeAssignedToCluster(MockedTestContext testContext) {
        LOGGER.info("testing recipe execution for type: {}", PRE_CLOUDERA_MANAGER_START.name());
        String recipeName = resourcePropertyProvider().getName();
        String stackName = resourcePropertyProvider().getName();
        String instanceGroupName = resourcePropertyProvider().getName();
        HostGroupType hostGroupTypeForRecipe = HostGroupType.WORKER;

        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(recipeName, RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generatePreDeploymentRecipeContent(applicationContext))
                    .withRecipeType(PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4(), RunningParameter.key(recipeName))
                .when(recipeTestClient.deleteV4(), RunningParameter.key(recipeName))
                .given(stackName, DistroXTestDto.class)
                .withRecipe(recipeName, hostGroupTypeForRecipe.getName())
                .whenException(distroXTestClient.create(), BadRequestException.class, expectedMessage(String.format("The given recipe does not exist" +
                        " for the instance group \"%s\": %s", hostGroupTypeForRecipe.getName(), recipeName)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created cluster with pretermination recipe",
            when = "calling termination",
            then = "the pretermination highstate has to called on pretermination recipes")
    public void testRecipePreTerminationRecipeHasGotHighStateOnCluster(MockedTestContext testContext) {
        String recipeName = resourcePropertyProvider().getName();
        String preCldrManagerStartRecipe = "pre-cldr-start-manager";
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generatePreTerminationRecipeContent(applicationContext))
                    .withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4())
                .given(RecipeTestDto.class)
                    .withName(preCldrManagerStartRecipe)
                    .withContent(recipeUtil.generatePreDeploymentRecipeContent(applicationContext))
                    .withRecipeType(PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4())
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .enableVerification()
                .await(STACK_AVAILABLE)
                .mockSalt().run().post().bodyContains(HIGHSTATE, 1).atLeast(1).verify()
                .given(DistroXTestDto.class)
                .withRecipes(recipeName, preCldrManagerStartRecipe)
                .when(distroXTestClient.attachRecipe())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(4))
                .await(STACK_AVAILABLE, pollingInterval(POLLING_INTERVAL))
                .mockSalt().run().post().bodyContains(HIGHSTATE, 1).atLeast(2).verify()
                .when(distroXTestClient.delete())
                .await(STACK_DELETED)
                .mockSalt().run().post().bodyContains(HIGHSTATE, 1).atLeast(2).verify()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created cluster with post ambari install recipe",
            when = "upscaling cluster",
            then = "the post recipe should run on the new nodes as well")
    public void testPostClusterInstallRecipeWhenClusterGetUpScaled(MockedTestContext testContext) {
        ApiParcel parcel = parcelGeneratorUtil.getActivatedCDHParcel();
        String clusterName = resourcePropertyProvider.getName();
        parcelMockActivatorUtil.mockActivateWithDefaultParcels(testContext, clusterName, parcel);
        String recipeName = resourcePropertyProvider().getName();
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generatePostDeploymentRecipeContent(applicationContext))
                    .withRecipeType(POST_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4())
                .given("cmpkey", DistroXClouderaManagerProductTestDto.class)
                    .withParcel("someParcel")
                    .withName(parcel.getProduct())
                    .withVersion(parcel.getVersion())
                .given("cmanager", DistroXClouderaManagerTestDto.class)
                    .withClouderaManagerProduct("cmpkey")
                .given("cmpclusterkey", DistroXClusterTestDto.class)
                    .withClouderaManager("cmanager")
                .given(DistroXTestDto.class)
                .withRecipe(recipeName)
                .withName(clusterName)
                .withCluster("cmpclusterkey")
                .when(distroXTestClient.create())
                .enableVerification()
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(4))
                .await(STACK_AVAILABLE, pollingInterval(POLLING_INTERVAL))
                .mockSalt().run().post().bodyContains(HIGHSTATE, 1).atLeast(1).verify()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created cluster with post ambari recipe",
            when = "upscaling cluster on hostgroup which has no post install recipe",
            then = "the post recipe should not run on the new nodes because those recipe not configured on the upscaled hostgroup")
    public void testPostCmStartRecipeNotExecutedWhenAnotherHostGroupGetUpScaled(
            MockedTestContext testContext) {
        ApiParcel parcel = parcelGeneratorUtil.getActivatedCDHParcel();
        String recipeName = resourcePropertyProvider().getName();
        String clusterName = resourcePropertyProvider.getName();
        parcelMockActivatorUtil.mockActivateWithDefaultParcels(testContext, clusterName, parcel);
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generatePostCmStartRecipeContent(applicationContext))
                    .withRecipeType(POST_CLOUDERA_MANAGER_START)
                .when(recipeTestClient.createV4())
                .given("cmpkey", DistroXClouderaManagerProductTestDto.class)
                    .withParcel("someParcel")
                    .withName(parcel.getProduct())
                    .withVersion(parcel.getVersion())
                .given("cmanager", DistroXClouderaManagerTestDto.class)
                    .withClouderaManagerProduct("cmpkey")
                .given("cmpclusterkey", DistroXClusterTestDto.class)
                    .withClouderaManager("cmanager")
                .given(DistroXTestDto.class)
                .withRecipe(recipeName)
                .withName(clusterName)
                .withCluster("cmpclusterkey")
                .when(distroXTestClient.create())
                .enableVerification()
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(4))
                .await(STACK_AVAILABLE, pollingInterval(POLLING_INTERVAL))
                .mockSalt().run().post().bodyContains(HIGHSTATE, 1).atLeast(1).verify()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created cluster with attached recipe",
            when = "delete attached recipe",
            then = "getting BadRequestException")
    public void testTryToDeleteAttachedRecipe(MockedTestContext testContext) {
        String recipeName = resourcePropertyProvider().getName();

        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generatePostCmStartRecipeContent(applicationContext))
                    .withRecipeType(POST_CLOUDERA_MANAGER_START)
                .when(recipeTestClient.createV4())
                .given(DistroXTestDto.class)
                .withRecipe(recipeName)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .given(RecipeTestDto.class)
                .whenException(recipeTestClient.deleteV4(), BadRequestException.class, expectedMessage("There is a cluster \\['.*'\\] which uses recipe" +
                        " '.*'. Please remove this cluster before deleting the recipe"))
                .validate();
    }

    @DataProvider(name = "dataProviderForNonPreTerminationRecipeTypes")
    public Object[][] getData() {
        return new Object[][]{
                {
                        getBean(MockedTestContext.class),
                        PRE_SERVICE_DEPLOYMENT,
                        2,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("pre ambari start recipes")
                                .when("calling cluster creation with the recipes")
                                .then("should run 2 times")
                },
                {
                        getBean(MockedTestContext.class),
                        POST_CLOUDERA_MANAGER_START,
                        1,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("post ambari start recipes")
                                .when("calling cluster creation with the recipes")
                                .then("should run 1 times")
                },
                {
                        getBean(MockedTestContext.class),
                        POST_SERVICE_DEPLOYMENT,
                        1,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("post cluster install recipes")
                                .when("calling cluster creation with the recipes")
                                .then("should run 1 times")
                }
        };
    }

}
