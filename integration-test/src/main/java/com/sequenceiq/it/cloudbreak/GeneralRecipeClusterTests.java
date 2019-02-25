package com.sequenceiq.it.cloudbreak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.Recipe;
import com.sequenceiq.it.cloudbreak.newway.priority.Priority;

public class GeneralRecipeClusterTests extends CloudbreakTest {

    private static final String CLUSTER_DEFINITION_HDP26_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String VALID_RECIPE_DESCRIPTION = "python recipe for API E2E tests";

    private static final Set<String> BASH_RECIPE_NAMES = new HashSet<>(Arrays.asList("post-ambari-start", "pre-ambari-start",
            "post-cluster-install", "pre-termination"));

    private static final Set<String> PYTHON_RECIPE_NAMES = new HashSet<>(Arrays.asList("post-ambari-start-py", "pre-ambari-start-py",
            "post-cluster-install-py", "pre-termination-py"));

    private static final String[] FILES_PATH = {"/var/log/recipes/post-ambari-start", "/var/log/recipes/pre-ambari-start",
            "/var/log/recipes/post-cluster-install"};

    private static final String[] HOSTGROUPS = {"master", "worker", "compute"};

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralRecipeClusterTests.class);

    private static final String PYTHON_SCRIPT = String.format("#!/usr/bin/python%nprint(\"An example of a python script\")%nimport sys%n"
            + "print(sys.version_info)");

    private static final String BASH_SCRIPT = String.format("#!/bin/bash%nCOUNTER=0%nwhile [ ! -f /tmp/pre-termination ] && [ $COUNTER -lt 15 ]%ndo%n  "
            + "sleep 2%n  COUNTER=$((COUNTER+1))%ndone");

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFile;

    private CloudProvider cloudProvider;

    public GeneralRecipeClusterTests() {
    }

    public GeneralRecipeClusterTests(CloudProvider cp, TestParameter tp) {
        cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@org.testng.annotations.Optional(OpenstackCloudProvider.OPENSTACK) String provider) throws Exception {
        LOGGER.info("before cluster test set provider: " + provider);
        if (cloudProvider == null) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        } else {
            LOGGER.info("cloud provider already set - running from factory test");
        }
        setUpRecipes();
    }

    @Test
    @Priority(10)
    public void testAClusterCreation() throws Exception {
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithClusterDefinitionName(CLUSTER_DEFINITION_HDP26_NAME)),
                "a cluster request");
        given(cloudProvider
                .aValidStackRequest()
                .withInstanceGroups(cloudProvider.instanceGroups(Stream.concat(
                        BASH_RECIPE_NAMES.stream(), PYTHON_RECIPE_NAMES.stream()).collect(Collectors.toSet())))
                .withName(getTestParameter().getWithDefault("openstackClusterName", "e2e-cluster-py-recipe")),
                "a stack request");
        when(Stack.postV3(), "post the stack request with recipes");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test()
    @Priority(20)
    public void testCheckRecipesOnNodes() throws Exception {
        LOGGER.info("Default Private Key File is: {}", defaultPrivateKeyFile);

        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated(), "a stack is created");
        when(Stack.get());
        then(Stack.checkRecipes(HOSTGROUPS, FILES_PATH, defaultPrivateKeyFile,
                getRequiredRecipeAmountForRunningCluster()), "check recipes are ran on all nodes");
    }

    @Test()
    @Priority(30)
    public void testTerminateClusterCheckRecipePreTerm() throws Exception {
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated(), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @AfterTest
    public void cleanUpdeleteValidRecipes() throws Exception {
        for (String recipe : Stream.concat(BASH_RECIPE_NAMES.stream(), PYTHON_RECIPE_NAMES.stream()).collect(Collectors.toSet())) {
            given(Recipe.request()
                    .withName(recipe)
            );
            when(Recipe.delete());
        }
    }

    private int getRequiredRecipeAmountForRunningCluster() {
        // * 2 since we post both bash and python script
        return FILES_PATH.length * 2 * HOSTGROUPS.length;
    }

    private void setUpRecipes() throws Exception {
        given(CloudbreakClient.created());
        for (String recipe : BASH_RECIPE_NAMES) {
            given(Recipe.isCreated()
                    .withName(recipe)
                    .withDescription(VALID_RECIPE_DESCRIPTION)
                    .withRecipeType(RecipeV4Type.valueOf(recipe.replace("-", "_").toUpperCase()))
                    .withContent(Base64.encodeBase64String(BASH_SCRIPT.getBytes()))
            );
        }
        for (String pythonRecipe : PYTHON_RECIPE_NAMES) {
            given(Recipe.isCreated()
                    .withName(pythonRecipe)
                    .withDescription(VALID_RECIPE_DESCRIPTION)
                    .withRecipeType(RecipeV4Type.valueOf(pythonRecipe.substring(0, pythonRecipe.indexOf("-py")).replace("-", "_").toUpperCase()))
                    .withContent(Base64.encodeBase64String(PYTHON_SCRIPT.getBytes()))
            );
        }
    }

}