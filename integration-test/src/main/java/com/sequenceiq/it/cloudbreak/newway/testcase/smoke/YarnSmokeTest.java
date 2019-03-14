package com.sequenceiq.it.cloudbreak.newway.testcase.smoke;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;

import javax.inject.Inject;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.newway.util.ShowClusterDefinitionUtil;

public class YarnSmokeTest extends AbstractE2ETest {

    private static final int NODE_COUNT = 1;

    private static final String INSTANCE_GROUP_ID = "ig";

    private static final String RECIPE_CONTENT = Base64.encodeBase64String("#!/bin/bash\necho TESZTELEK".getBytes());

    @Inject
    private RandomNameCreator randomNameCreator;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a valid YARN cluster",
            when = "checking the generated Cluster Definition",
            then = "the Cluster Definition should be present and a valid json"
    )
    public void testWhenCreatedYARNClusterClusterDefinitionShouldBeGenerated(TestContext testContext) {
        testContext.given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.getV4())
                .then(ShowClusterDefinitionUtil::checkGeneratedClusterDefinition)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false, description = "Upscaling is not supported on YARN cloudplatform")
    @Description(
            given = "a valid YARN cluster",
            when = "upscaling the cluster with 15 nodes",
            then = "the cluster should be available and scaled up successfully"
    )
    public void testWhenCreatedYARNClusterShouldBeUpScaled(TestContext testContext) {
        testContext.given(StackTestDto.class)
                .when(stackTestClient.clusterDefinitionRequestV4())
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(15))
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false, description = "Stop is not supported on YARN cloudplatform")
    @Description(
            given = "a valid YARN cluster",
            when = "stopping then starting the cluster",
            then = "the cluster should be available and successfully started"
    )
    public void testWhenCreatedYARNClusterStoppedStarted(TestContext testContext) {
        testContext.given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.stopV4())
                .await(STACK_STOPPED)
                .when(stackTestClient.startV4())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a valid YARN cluster with PRE_TERMINATION recipe",
            when = "terminate the cluster",
            then = "the pre-termination highstate has to be called"
    )
    public void testWhenCreatedYARNClusterShouldPreTerminationRecipePresent(TestContext testContext) {
        String recipeName = randomNameCreator.getRandomNameForResource();
        testContext.given(RecipeTestDto.class)
                .withName(recipeName).withContent(RECIPE_CONTENT).withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4())
                .given(INSTANCE_GROUP_ID, InstanceGroupEntity.class)
                .withHostGroup(WORKER).withNodeCount(NODE_COUNT).withRecipes(recipeName)
                .given(StackTestDto.class)
                .replaceInstanceGroups(INSTANCE_GROUP_ID)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.deleteV4())
                .await(STACK_DELETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false, description = "Upscaling is not supported on YARN cloudplatform")
    @Description(
            given = "a valid YARN cluster with POST_CLUSTER_INSTALL, PRE_AMBARI_START, POST_AMBARI_START, PRE_TERMINATION recipes",
            when = "upscaling the cluster with 2 nodes",
            then = "the cluster should be available and scaled up successfully"
    )
    public void testWhenCreatedYARNClusterShouldBeUpScaledAlongWithAllTheRecipes(TestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .withContent(RECIPE_CONTENT).withRecipeType(POST_CLUSTER_INSTALL).withRecipeType(PRE_AMBARI_START).withRecipeType(POST_AMBARI_START)
                .withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4())
                .given(INSTANCE_GROUP_ID, InstanceGroupEntity.class)
                .withHostGroup(WORKER).withNodeCount(NODE_COUNT)
                .given(StackTestDto.class)
                .replaceInstanceGroups(INSTANCE_GROUP_ID)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(2))
                .await(STACK_AVAILABLE)
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void teardown(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}