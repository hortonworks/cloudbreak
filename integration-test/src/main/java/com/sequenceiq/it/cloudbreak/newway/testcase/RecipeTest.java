package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.SaltMock.SALT_RUN;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.Recipe;
import com.sequenceiq.it.cloudbreak.newway.RecipeEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;

public class RecipeTest extends AbstractIntegrationTest {

    private static final String WORKER_ID = "ig";

    private static final String COMPUTE_ID = "ig";

    private static final String TEST_CONTEXT = "testContext";

    private static final String HIGHSTATE = "state.highstate";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTest.class);

    private static final String RECIPE_CONTENT = Base64.encodeBase64String("#!/bin/bash\necho ALMAA".getBytes());

    @Inject
    private RandomNameCreator creator;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = "dataProviderForNonPreTerminationRecipeTypes")
    public void testRecipeNotPreTerminationHasGotHighStateOnCluster(TestContext testContext, HostGroupType hostGroup, int nodeCount,
                                                                    RecipeV4Type type, int executionTime) {
        LOGGER.debug("testing recipe execution for type: {}", type.name());
        String recipeName = creator.getRandomNameForMock();
        testContext
                .given(RecipeEntity.class).withName(recipeName).withContent(RECIPE_CONTENT).withRecipeType(type)
                .when(Recipe.postV2())
                .given(WORKER_ID, InstanceGroupEntity.class).withHostGroup(hostGroup).withNodeCount(nodeCount).withRecipes(recipeName)
                .given(StackEntity.class).replaceInstanceGroups(WORKER_ID)
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(executionTime))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testRecipePreTerminationRecipeHasGotHighStateOnCluster(TestContext testContext) {
        String recipeName = creator.getRandomNameForMock();
        testContext
                .given(RecipeEntity.class).withName(recipeName).withContent(RECIPE_CONTENT).withRecipeType(PRE_TERMINATION)
                .when(Recipe.postV2())
                .given(WORKER_ID, InstanceGroupEntity.class).withHostGroup(WORKER).withNodeCount(1).withRecipes(recipeName)
                .given(StackEntity.class).replaceInstanceGroups(WORKER_ID)
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(2))
                .when(Stack.deleteV2())
                .await(STACK_DELETED)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(3))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, description = "Post Ambari start recipes executed when LDAP config is present, because later the LDAP sync is "
            + "hooked for this salt state in the top.sls")
    public void testWhenThereIsNoRecipeButLdapHasAttachedThenThePostAmbariRecipeShouldRunWhichResultThreeHighStateCall(TestContext testContext) {
        String ldapName = creator.getRandomNameForMock();
        testContext
                .given(LdapConfigEntity.class).withName(ldapName)
                .when(LdapConfig.postV2())
                .given(StackEntity.class).withCluster(new ClusterEntity(testContext).valid().withLdapConfigName(ldapName))
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(3))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testWhenClusterGetUpscaledThenPostClusterInstallRecipeShouldBeExecuted(TestContext testContext) {
        String recipeName = creator.getRandomNameForMock();
        testContext
                .given(RecipeEntity.class).withName(recipeName).withContent(RECIPE_CONTENT).withRecipeType(POST_CLUSTER_INSTALL)
                .when(Recipe.postV2())
                .given(WORKER_ID, InstanceGroupEntity.class).withHostGroup(WORKER).withNodeCount(1).withRecipes(recipeName)
                .given(StackEntity.class).replaceInstanceGroups(WORKER_ID)
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(2))
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(4))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testWhenRecipeProvidedToHostGroupAndAnotherHostgroupGetUpscaledThenThereIsNoFurtherRecipeExecutionOnTheNewNodeBesideTheDefaultOnes(
            TestContext testContext) {
        String recipeName = creator.getRandomNameForMock();
        testContext
                .given(RecipeEntity.class).withName(recipeName).withContent(RECIPE_CONTENT).withRecipeType(POST_AMBARI_START)
                .when(Recipe.postV2())
                .given(COMPUTE_ID, InstanceGroupEntity.class).withHostGroup(COMPUTE).withNodeCount(1).withRecipes(recipeName)
                .given(StackEntity.class).replaceInstanceGroups(COMPUTE_ID)
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(2))
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(5))
                .validate();
    }

    @DataProvider(name = "dataProviderForNonPreTerminationRecipeTypes")
    public Object[][] getData() {
        return new Object[][]{
                {applicationContext.getBean(TestContext.class), WORKER, 1, PRE_AMBARI_START, 3},
                {applicationContext.getBean(TestContext.class), WORKER, 1, POST_AMBARI_START, 3},
                {applicationContext.getBean(TestContext.class), WORKER, 1, POST_CLUSTER_INSTALL, 2}
        };
    }

}