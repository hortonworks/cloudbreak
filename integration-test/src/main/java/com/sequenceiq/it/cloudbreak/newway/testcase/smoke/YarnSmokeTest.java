package com.sequenceiq.it.cloudbreak.newway.testcase.smoke;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_AMBARI_START;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;

import java.io.IOException;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.newway.util.AmbariUtil;
import com.sequenceiq.it.util.ResourceUtil;

public class YarnSmokeTest extends AbstractE2ETest {

    private static final int NODE_COUNT = 1;

    private static final String INSTANCE_GROUP_ID = "ig";

    private static final String CREATE_AMBARI_USER_SCRIPT_FILE = "classpath:/recipes/create-ambari-user.sh";

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a valid YARN cluster with POST_AMBARI_STAR recipe",
            when = "the cluster is created",
            then = "a new Ambari user has to be created"
    )
    public void testWhenCreatedYARNClusterShouldAmbariUserPresentByPostAmbariInstallRecipe(TestContext testContext) throws IOException {
        String postAmbariStartRecipeName = resourcePropertyProvider().getName();

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

    private String generateCreateAmbariUserRecipeContent(String filePath) throws IOException {
        String ambariUser = commonCloudProperties.getAmbari().getDefaultUser();
        String ambariPassword = commonCloudProperties.getAmbari().getDefaultPassword();
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, filePath);

        recipeContentFromFile = recipeContentFromFile.replaceAll("AMBARI_USER", ambariUser);
        recipeContentFromFile = recipeContentFromFile.replaceAll("AMBARI_PASSWORD", ambariPassword);
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }
}