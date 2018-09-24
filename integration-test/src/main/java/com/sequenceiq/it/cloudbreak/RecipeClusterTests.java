package com.sequenceiq.it.cloudbreak;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Recipe;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RecipeClusterTests extends CloudbreakTest {

    private static final String BLUEPRINT_HDP26_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String VALID_RECIPE_DESCRIPTION = "recipe for API E2E tests";

    private static final Set<String> RECIPE_NAMES = new HashSet<>(Arrays.asList("post-ambari-start", "pre-ambari-start",
            "post-cluster-install", "pre-termination"));

    private static final String INVALID_RECIPE_NAME = "recipe-exit-1";

    private static final String[] FILES_PATH = {"/tmp/post-ambari-start", "/tmp/pre-ambari-start", "/tmp/post-cluster-install"};

    private static final String[] FILES_PATH_PRE_TERM = {"/tmp/pre-termination"};

    private static final String[] HOSTGROUPS = {"master", "worker", "compute"};

    private static final String[] HOSTGROUP_MASTER = {"master"};

    private static final String INVALID_CLUSTER_NAME = "e2e-cluster-invalid-recipe";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeClusterTests.class);

    private static final String WAIT_SCRIPT_PATH = " /home/cloudbreak/waitscript.sh;";

    private static final String WAIT_SCRIPT = "#!/bin/bash\n"
            + "COUNTER=0\n"
            + "while [ ! -f /tmp/pre-termination ] && [ $COUNTER -lt 15 ]\n"
            + "do\n"
            + "  sleep 2\n"
            + "  COUNTER=$((COUNTER+1))\n"
            + "done";

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFile;

    private CloudProvider cloudProvider;

    public RecipeClusterTests() {
    }

    public RecipeClusterTests(CloudProvider cp, TestParameter tp) {
        cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (cloudProvider == null) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        } else {
            LOGGER.info("cloud provider already set - running from factory test");
        }
    }

    @BeforeTest
    public void setupValidRecipes() throws Exception {
        given(CloudbreakClient.created());
        for (String recipe : RECIPE_NAMES) {
            given(Recipe.isCreated()
                    .withName(recipe)
                    .withDescription(VALID_RECIPE_DESCRIPTION)
                    .withRecipeType(RecipeType.valueOf(recipe.replace("-", "_").toUpperCase()))
                    .withContent(Base64.encodeBase64String(("#!/bin/bash" + "\ntouch /tmp/" + recipe).getBytes()))
            );
        }
    }

    @BeforeTest
    public void setupInvalidRecipe() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.isCreated()
                .withName(INVALID_RECIPE_NAME)
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeType.POST_AMBARI_START)
                .withContent(Base64.encodeBase64String("#!/bin/bash \nexit -1".getBytes()))
        );
    }

    @Test
    public void testCreateClusterWithRecipes() throws Exception {
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(BLUEPRINT_HDP26_NAME)),
                "a cluster request");
        given(cloudProvider.aValidStackRequest().withInstanceGroups(cloudProvider.instanceGroups(RECIPE_NAMES)),  "a stack request");
        when(Stack.post(), "post the stack request with recipes");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(priority = 1)
    public void testCheckRecipesOnNodes() throws Exception {
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated(), "a stack is created");
        when(Stack.get());
        then(Stack.checkRecipes(HOSTGROUPS, FILES_PATH, defaultPrivateKeyFile, "ls ", 9), "check recipes are ran on nodes");
    }

    @Test(priority = 2, expectedExceptions = BadRequestException.class)
    public void testTryToDeleteAttachedRecipe() throws Exception {
        for (String recipe: RECIPE_NAMES) {
            given(Recipe.request()
                    .withName(recipe)
            );
            when(Recipe.delete());
        }
    }

    @Test(priority = 3)
    public void testTerminateClusterCheckRecipePreTerm() throws Exception {
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated(), "a stack is created");
        when(Stack.delete());
        then(Stack.checkRecipes(HOSTGROUP_MASTER, FILES_PATH_PRE_TERM, defaultPrivateKeyFile, "echo '" + WAIT_SCRIPT + "' > " + WAIT_SCRIPT_PATH
                        + "chmod 777 " + WAIT_SCRIPT_PATH + WAIT_SCRIPT_PATH + "ls ", 1),
                "check pre termination recipe on master node");
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Test(priority = 4)
    public void testCreateClusterWithInvalidRecipes() throws Exception {
        Set<String> invalidRecipe = new HashSet<>();
        invalidRecipe.add(INVALID_RECIPE_NAME);
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(BLUEPRINT_HDP26_NAME)),
                "a cluster request");
        given(cloudProvider.aValidStackRequest().withInstanceGroups(cloudProvider.instanceGroups(invalidRecipe)).withName(INVALID_CLUSTER_NAME),
                "a stack request");
        when(Stack.post(), "post the stack request with an invalid recipe");
        then(Stack.waitAndCheckClusterFailure(INVALID_RECIPE_NAME), "check cluster failed with given failure message");
    }

    @Test(priority = 5)
    public void cleanUpTerminateFailedCluster() throws Exception {
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(INVALID_CLUSTER_NAME), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Test(priority = 6)
    public void cleanUpdeleteValidRecipes() throws Exception {
        for (String recipe : RECIPE_NAMES) {
            given(Recipe.request()
                    .withName(recipe)
            );
            when(Recipe.delete());
        }
    }

    @Test(priority = 7)
    public void cleanUpdeleteInValidRecipe() throws Exception {
        given(Recipe.request()
                .withName(INVALID_RECIPE_NAME)
        );
        when(Recipe.delete());
    }
}