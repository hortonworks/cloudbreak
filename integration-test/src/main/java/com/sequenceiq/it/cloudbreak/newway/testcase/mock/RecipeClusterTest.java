package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.SaltMock.SALT_RUN;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.action.recipe.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.action.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.LdapConfigTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class RecipeClusterTest extends AbstractIntegrationTest {

    private static final int NODE_COUNT = 1;

    private static final String INSTANCE_GROUP_ID = "ig";

    private static final String HIGHSTATE = "state.highstate";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeClusterTest.class);

    private static final String RECIPE_CONTENT = Base64.encodeBase64String("#!/bin/bash\necho ALMAA".getBytes());

    @Inject
    private LdapConfigTestClient ldapConfigTestClient;

    @Inject
    private RandomNameCreator creator;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((TestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = "dataProviderForNonPreTerminationRecipeTypes")
    public void testRecipeNotPreTerminationHasGotHighStateOnCluster(
            TestContext testContext,
            RecipeV4Type type,
            int executionTime,
            @Description TestCaseDescription testCaseDescription) {
        LOGGER.info("testing recipe execution for type: {}", type.name());
        String recipeName = creator.getRandomNameForResource();
        String stackName = creator.getRandomNameForResource();
        String instanceGroupName = creator.getRandomNameForResource();

        testContext
                .given(recipeName, RecipeTestDto.class)
                .withName(recipeName)
                .withContent(RECIPE_CONTENT)
                .withRecipeType(type)
                .when(RecipeTestClient::postV4, key(recipeName))
                .given(instanceGroupName, InstanceGroupEntity.class)
                .withHostGroup(WORKER)
                .withNodeCount(NODE_COUNT)
                .withRecipes(recipeName)
                .given(stackName, StackTestDto.class)
                .replaceInstanceGroups(instanceGroupName)
                .when(Stack.postV4(), key(stackName))
                .await(STACK_AVAILABLE, key(stackName))
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).atLeast(executionTime), key(stackName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created cluster with pretermination recipe",
            when = "calling termination",
            then = "the pretermination highstate has to called on pretermination recipes")
    public void testRecipePreTerminationRecipeHasGotHighStateOnCluster(TestContext testContext) {
        String recipeName = creator.getRandomNameForResource();
        testContext
                .given(RecipeTestDto.class)
                .withName(recipeName)
                .withContent(RECIPE_CONTENT)
                .withRecipeType(PRE_TERMINATION)
                .when(RecipeTestClient::postV4)
                .given(INSTANCE_GROUP_ID, InstanceGroupEntity.class)
                .withHostGroup(WORKER)
                .withNodeCount(NODE_COUNT)
                .withRecipes(recipeName)
                .given(StackTestDto.class)
                .replaceInstanceGroups(INSTANCE_GROUP_ID)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(2))
                .when(Stack.deleteV4())
                .await(STACK_DELETED)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(3))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created cluster with post ambari install recipe and ldap attached to the cluster",
            when = "creating cluster",
            then = "the LDAP sync is hooked for this salt state in the top.sls")
    public void testWhenThereIsNoRecipeButLdapHasAttachedThenThePostAmbariRecipeShouldRunWhichResultThreeHighStateCall(TestContext testContext) {
        String ldapName = creator.getRandomNameForResource();
        testContext
                .given(LdapConfigTestDto.class)
                .withName(ldapName)
                .when(ldapConfigTestClient.post())
                .given(StackTestDto.class)
                .withCluster(new ClusterEntity(testContext).valid().withLdapConfigName(ldapName))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(3))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created cluster with post ambari install recipe",
            when = "upscaling cluster",
            then = "the post recipe should run on the new nodes as well")
    public void testWhenClusterGetUpScaledThenPostClusterInstallRecipeShouldBeExecuted(TestContext testContext) {
        String recipeName = creator.getRandomNameForResource();
        testContext
                .given(RecipeTestDto.class)
                .withName(recipeName)
                .withContent(RECIPE_CONTENT)
                .withRecipeType(POST_CLUSTER_INSTALL)
                .when(RecipeTestClient::postV4)
                .given(INSTANCE_GROUP_ID, InstanceGroupEntity.class)
                .withHostGroup(WORKER)
                .withNodeCount(NODE_COUNT)
                .withRecipes(recipeName)
                .given(StackTestDto.class)
                .replaceInstanceGroups(INSTANCE_GROUP_ID)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(2))
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(4))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created cluster with post ambari recipe",
            when = "upscaling cluster on hostgroup which has no post install recipe",
            then = "the post recipe should not run on the new nodes because those recipe not configured on the upscaled hostgroup")
    public void testWhenRecipeProvidedToHostGroupAndAnotherHostGroupGetUpScaledThenThereIsNoFurtherRecipeExecutionOnTheNewNodeBesideTheDefaultOnes(
            TestContext testContext) {
        String recipeName = creator.getRandomNameForResource();
        testContext
                .given(RecipeTestDto.class)
                .withName(recipeName)
                .withContent(RECIPE_CONTENT)
                .withRecipeType(POST_AMBARI_START)
                .when(RecipeTestClient::postV4)
                .given(INSTANCE_GROUP_ID, InstanceGroupEntity.class)
                .withHostGroup(COMPUTE)
                .withNodeCount(NODE_COUNT)
                .withRecipes(recipeName)
                .given(StackTestDto.class)
                .replaceInstanceGroups(INSTANCE_GROUP_ID)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(2))
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(5))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created cluster with attached recipe",
            when = "delete attached recipe",
            then = "getting BadRequestException")
    public void testTryToDeleteAttachedRecipe(TestContext testContext) {
        String recipeName = creator.getRandomNameForResource();
        testContext
                .given(RecipeTestDto.class).withName(recipeName).withContent(RECIPE_CONTENT).withRecipeType(POST_AMBARI_START)
                .when(RecipeTestClient::postV4)
                .given(INSTANCE_GROUP_ID, InstanceGroupEntity.class).withRecipes(recipeName)
                .given(StackTestDto.class).replaceInstanceGroups(INSTANCE_GROUP_ID)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)

                .given(RecipeTestDto.class)
                .when(RecipeTestClient::deleteV4, key("delete-failed-recipe"))
                .expect(BadRequestException.class, key("delete-failed-recipe")
                        .withExpectedMessage("There is a cluster \\['.*'\\] which uses recipe '.*'. "
                                + "Please remove this cluster before deleting the recipe"))
                .validate();
    }

    @DataProvider(name = "dataProviderForNonPreTerminationRecipeTypes")
    public Object[][] getData() {
        return new Object[][]{
                {
                        getBean(MockedTestContext.class),
                        PRE_AMBARI_START,
                        3,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("pre ambari start recipes")
                                .when("calling cluster creation with the recipes")
                                .then("should run 3 times")
                },
                {
                        getBean(MockedTestContext.class),
                        POST_AMBARI_START,
                        3,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("post ambari start recipes")
                                .when("calling cluster creation with the recipes")
                                .then("should run 3 times")
                },
                {
                        getBean(MockedTestContext.class),
                        POST_CLUSTER_INSTALL,
                        2,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("post cluster install recipes")
                                .when("calling cluster creation with the recipes")
                                .then("should run 2 times")
                }
        };
    }

}