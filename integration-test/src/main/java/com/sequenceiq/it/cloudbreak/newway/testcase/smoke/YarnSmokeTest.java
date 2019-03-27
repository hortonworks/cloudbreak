package com.sequenceiq.it.cloudbreak.newway.testcase.smoke;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.newway.util.AmbariUtil;
import com.sequenceiq.it.cloudbreak.newway.util.ShowClusterDefinitionUtil;
import com.sequenceiq.it.util.ResourceUtil;

public class YarnSmokeTest extends AbstractE2ETest {

    private static final int NODE_COUNT = 1;

    private static final String INSTANCE_GROUP_ID = "ig";

    private static final String POST_CLUSTER_INSTALL_RECIPE_SCRIPT_FILE = "classpath:/recipes/post-install.sh";

    private static final String PRE_AMBARI_START_RECIPE_SCRIPT_FILE = "classpath:/recipes/pre-ambari.sh";

    private static final String POST_AMBARI_START_RECIPE_SCRIPT_FILE = "classpath:/recipes/post-ambari.sh";

    private static final String PRE_TERMINATION_RECIPE_SCRIPT_FILE = "classpath:/recipes/pre-termination.sh";

    private static final String CREATE_AMBARI_USER_SCRIPT_FILE = "classpath:/recipes/create-ambari-user.sh";

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnSmokeTest.class);

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false, description = "Gatekeeper only")
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
            given = "a valid YARN cluster with POST_AMBARI_STAR recipe",
            when = "the cluster is created",
            then = "a new Ambari user has to be created"
    )
    public void testWhenCreatedYARNClusterShouldAmbariUserPresentByPostAmbariInstallRecipe(TestContext testContext) throws IOException {
        String postAmbariStartRecipeName = getNameGenerator().getRandomNameForResource();

        testContext.given(RecipeTestDto.class)
                .withName(postAmbariStartRecipeName).withContent(generateCreateAmbariUserRecipeContent(CREATE_AMBARI_USER_SCRIPT_FILE))
                .withRecipeType(POST_AMBARI_START)
                .when(recipeTestClient.createV4())
                .given(INSTANCE_GROUP_ID, InstanceGroupTestDto.class)
                .withHostGroup(MASTER).withNodeCount(NODE_COUNT).withRecipes(postAmbariStartRecipeName)
                .given(StackTestDto.class)
                .replaceInstanceGroups(INSTANCE_GROUP_ID)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(AmbariUtil::checkAmbariUser)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false, description = "Upscaling is not supported on YARN cloudplatform")
    @Description(
            given = "a valid YARN cluster with POST_CLUSTER_INSTALL, PRE_AMBARI_START, POST_AMBARI_START, PRE_TERMINATION recipes",
            when = "upscaling the cluster with 2 nodes",
            then = "the cluster should be available and scaled up successfully"
    )
    public void testWhenCreatedYARNClusterShouldBeUpScaledAlongWithAllTheRecipes(TestContext testContext) throws IOException {
        String postClusterInstallRecipeName = getNameGenerator().getRandomNameForResource();
        String preAmbariStartRecipeName = getNameGenerator().getRandomNameForResource();
        String postAmbariStartRecipeName = getNameGenerator().getRandomNameForResource();
        String preTerminationRecipeName = getNameGenerator().getRandomNameForResource();
        String[] recipeNames = new String[]{postClusterInstallRecipeName, preAmbariStartRecipeName, postAmbariStartRecipeName, preTerminationRecipeName};

        testContext.given(RecipeTestDto.class)
                .withName(postClusterInstallRecipeName)
                .withContent(Base64.encodeBase64String(ResourceUtil
                        .readResourceAsString(applicationContext, POST_CLUSTER_INSTALL_RECIPE_SCRIPT_FILE).getBytes()))
                .withRecipeType(POST_CLUSTER_INSTALL)
                .when(recipeTestClient.createV4())
                .withName(preAmbariStartRecipeName)
                .withContent(Base64.encodeBase64String(ResourceUtil
                        .readResourceAsString(applicationContext, PRE_AMBARI_START_RECIPE_SCRIPT_FILE).getBytes()))
                .withRecipeType(PRE_AMBARI_START)
                .when(recipeTestClient.createV4())
                .withName(postAmbariStartRecipeName)
                .withContent(Base64.encodeBase64String(ResourceUtil
                        .readResourceAsString(applicationContext, POST_AMBARI_START_RECIPE_SCRIPT_FILE).getBytes()))
                .withRecipeType(POST_AMBARI_START)
                .when(recipeTestClient.createV4())
                .withName(preTerminationRecipeName)
                .withContent(Base64.encodeBase64String(ResourceUtil
                        .readResourceAsString(applicationContext, PRE_TERMINATION_RECIPE_SCRIPT_FILE).getBytes()))
                .withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4())
                .given(INSTANCE_GROUP_ID, InstanceGroupTestDto.class)
                .withHostGroup(WORKER).withNodeCount(NODE_COUNT).withRecipes(recipeNames)
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
        testContext.cleanupTestContext();
    }

    private String generateCreateAmbariUserRecipeContent(String filePath) throws IOException {
        String ambariUser = getTestParameter().get(CommonCloudParameters.DEFAULT_AMBARI_USER);
        String ambariPassword = getTestParameter().get(CommonCloudParameters.DEFAULT_AMBARI_PASSWORD);
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, filePath);

        recipeContentFromFile = recipeContentFromFile.replaceAll("AMBARI_USER", ambariUser);
        recipeContentFromFile = recipeContentFromFile.replaceAll("AMBARI_PASSWORD", ambariPassword);
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }
}