package com.sequenceiq.it.cloudbreak.newway.testcase.smoke;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.SaltMock.SALT_RUN;

import javax.inject.Inject;

import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.it.cloudbreak.newway.action.stack.StackTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.newway.util.ShowClusterDefinitionUtil;

public class YarnSmokeTest extends AbstractIntegrationTest {

    private static final int NODE_COUNT = 1;

    private static final String INSTANCE_GROUP_ID = "ig";

    private static final String HIGHSTATE = "state.highstate";

    private static final String RECIPE_CONTENT = Base64.encodeBase64String("#!/bin/bash\necho TESZTELEK".getBytes());

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK,
            description = "A valid YARN cluster should be created with a valid generated cluster definition")
    public void testWhenCreatedYARNClusterClusterDefinitionShouldBeGenerated(MockedTestContext testContext) {
        testContext.given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.getV4())
                .then(ShowClusterDefinitionUtil::checkGeneratedClusterDefinition)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false,
        description = "A valid YARN cluster should be successfully scaled up")
    public void testWhenCreatedYARNClusterShouldBeUpScaled(TestContext testContext) {
        testContext.given(StackTestDto.class)
                .when(stackTestClient.clusterDefinitionRequestV4())
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(15))
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false,
            description = "A valid YARN cluster should be able to stopped and then started")
    public void testWhenCreatedYARNClusterStoppedStarted(MockedTestContext testContext) {
        testContext.given(StackTestDto.class)
                .when(StackTestAction::create)
                .await(STACK_AVAILABLE)
                .when(StackTestAction::stop)
                .await(STACK_STOPPED)
                .when(StackTestAction::start)
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false,
            description = "A valid YARN cluster should be scaled up successfully with all the defined recipes")
    public void testWhenCreatedYARNClusterShouldBeUpScaledAlongWithAllTheRecipes(TestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .withContent(RECIPE_CONTENT)
                .withRecipeType(POST_CLUSTER_INSTALL)
                .withRecipeType(PRE_AMBARI_START)
                .withRecipeType(POST_AMBARI_START)
                .withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4())
                .given(INSTANCE_GROUP_ID, InstanceGroupEntity.class)
                .withHostGroup(WORKER)
                .withNodeCount(NODE_COUNT)
                .given(StackTestDto.class)
                .replaceInstanceGroups(INSTANCE_GROUP_ID)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(2))
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(HIGHSTATE).exactTimes(4))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void teardown(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}