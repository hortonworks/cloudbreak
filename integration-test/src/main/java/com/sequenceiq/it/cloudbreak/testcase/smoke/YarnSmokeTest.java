package com.sequenceiq.it.cloudbreak.testcase.smoke;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.ClouderaManagerUtil;
import com.sequenceiq.it.util.ResourceUtil;

public class YarnSmokeTest extends AbstractE2ETest {

    private static final int NODE_COUNT = 1;

    private static final String INSTANCE_GROUP_ID = "ig";

    private static final String CREATE_CM_USER_SCRIPT_FILE = "classpath:/recipes/create-cm-user.sh";

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a valid YARN workload with POST CLUSTER MANAGER START recipe",
            when = "the workload is created",
            then = "a new Cloudera Manager user has to be created"
    )
    public void testWhenCreatedYARNClusterShouldClouderaManagerUserPresentByPostClusterInstallRecipe(TestContext testContext) throws IOException {
        String postCmStartRecipeName = resourcePropertyProvider().getName();
        String cm = resourcePropertyProvider().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        testContext
                .given(cm, ClouderaManagerTestDto.class)
                .given(cmcluster, ClusterTestDto.class).withValidateBlueprint(Boolean.FALSE).withClouderaManager(cm)
                .given(RecipeTestDto.class)
                .withName(postCmStartRecipeName).withContent(generateCreateCMUserRecipeContent(CREATE_CM_USER_SCRIPT_FILE))
                .withRecipeType(POST_CLOUDERA_MANAGER_START)
                .when(recipeTestClient.createV4())
                .given(INSTANCE_GROUP_ID, InstanceGroupTestDto.class)
                .withHostGroup(MASTER).withNodeCount(NODE_COUNT).withRecipes(postCmStartRecipeName)
                .given(StackTestDto.class).withCluster(cmcluster)
                .replaceInstanceGroups(INSTANCE_GROUP_ID)
                .when(stackTestClient.createV4(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .then(ClouderaManagerUtil::checkClouderaManagerUser)
                .validate();
    }

    private String generateCreateCMUserRecipeContent(String filePath) throws IOException {
        String cmUser = commonCloudProperties.getClouderaManager().getDefaultUser();
        String cmPassword = commonCloudProperties.getClouderaManager().getDefaultPassword();
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, filePath);

        recipeContentFromFile = recipeContentFromFile.replaceAll("CM_USER", cmUser);
        recipeContentFromFile = recipeContentFromFile.replaceAll("CM_PASSWORD", cmPassword);
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }
}