package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeExecutionFailureCollector.RecipeFailure;

public class RecipeExecutionFailureCollectorTest {

    private static final String EXCEPTION_MESSAGE = "\"Comment: Command \"/opt/scripts/recipe-runner.sh post-ambari-start failingRecipe1\" run\n"
            + "Stdout: /opt/scripts/recipe-runner.sh post-ambari-start failingRecipe1 : Timed out after 10 seconds\""
            + "\"Comment: Command \"/opt/scripts/recipe-runner.sh pre-ambari-start failingRecipe2\" run\n"
            + "Stdout: /opt/scripts/recipe-runner.sh pre-ambari-start failingRecipe2 : Timed out after 10 seconds\""
            + "Comment: One or more requisite failed: postgresql.init-services-db, postgresql.configure-listen-address"
            + "Comment: One or more requisite failed: postgresql.init-services-db, postgresql.configure-listen-address";

    private final RecipeExecutionFailureCollector recipeExecutionFailureHandler = new RecipeExecutionFailureCollector();

    @Test
    public void testCanProcessWithUnprocessableMessage() {
        ArrayListMultimap<String, String> nodesWithErrors = getNodesWithErrors();
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("Something went wrong", nodesWithErrors);
        assertFalse(recipeExecutionFailureHandler.canProcessExecutionFailure(exception));
    }

    @Test
    public void testCanProcessProcessableMessage() {
        ArrayListMultimap<String, String> nodesWithErrors = getNodesWithErrors();
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException(EXCEPTION_MESSAGE, nodesWithErrors);
        assertTrue(recipeExecutionFailureHandler.canProcessExecutionFailure(exception));
    }

    @Test
    public void testCollectErrors() {
        ArrayListMultimap<String, String> nodesWithErrors = getNodesWithErrors();
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException(EXCEPTION_MESSAGE, nodesWithErrors);

//        Recipe failingRecipe1 = new Recipe();
//        failingRecipe1.setName("failingRecipe1");
//        Recipe failingRecipe2 = new Recipe();
//        failingRecipe2.setName("failingRecipe2");
//        Recipe goodRecipe = new Recipe();
//        goodRecipe.setName("goodRecipe");

//        HostGroup master = new HostGroup();
//        master.setName("master");
//        master.setRecipes(Sets.newHashSet(failingRecipe1, goodRecipe));

//        HostGroup worker = new HostGroup();
//        worker.setName("worker");
//        worker.setRecipes(Sets.newHashSet(failingRecipe1, failingRecipe2));

//        RecipeModel failingRecipeModel1 = new RecipeModel("failingRecipe1", POST_AMBARI_START, "");
//        RecipeModel failingRecipeModel2 = new RecipeModel("failingRecipe2", PRE_AMBARI_START, "");
//        RecipeModel goodRecipeModel = new RecipeModel("goodRecipe", POST_AMBARI_START, "");

        List<RecipeFailure> recipeExecutionFailures = recipeExecutionFailureHandler.collectErrors(exception);

        assertEquals(3, recipeExecutionFailures.size());

        long recipe1Failures = recipeExecutionFailures.stream()
                .filter(failure -> "failingRecipe1".equals(failure.getRecipeName()))
                .count();
        assertEquals(2, recipe1Failures);

        long recipe2Failures = recipeExecutionFailures.stream()
                .filter(failure -> "failingRecipe2".equals(failure.getRecipeName()))
                .count();
        assertEquals(1, recipe2Failures);

        long workerInstanceFailures = recipeExecutionFailures.stream()
                .filter(failure -> "host-10-0-0-4.openstacklocal".equals(failure.getHost()))
                .count();
        assertEquals(2, workerInstanceFailures);

        long masterInstanceFailures = recipeExecutionFailures.stream()
                .filter(failure -> "host-10-0-0-3.openstacklocal".equals(failure.getHost()))
                .peek(failure -> assertEquals("failingRecipe1", failure.getRecipeName()))
                .count();
        assertEquals(1, masterInstanceFailures);
    }

    private ArrayListMultimap<String, String> getNodesWithErrors() {
        ArrayListMultimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.putAll("host-10-0-0-4.openstacklocal", Arrays.asList(
                "\"Comment: Command \"/opt/scripts/recipe-runner.sh post-ambari-start failingRecipe1\" run\n"
                        + "Stdout: /opt/scripts/recipe-runner.sh post-ambari-start failingRecipe1 : Timed out after 10 seconds\"",
                "\"Comment: Command \"/opt/scripts/recipe-runner.sh pre-ambari-start failingRecipe2\" run\n"
                        + "Stdout: /opt/scripts/recipe-runner.sh pre-ambari-start failingRecipe2 : Timed out after 10 seconds\"",
                "Comment: One or more requisite failed: postgresql.init-services-db, postgresql.configure-listen-address",
                "Comment: One or more requisite failed: postgresql.init-services-db, postgresql.configure-listen-address"
        ));
        nodesWithErrors.put("host-10-0-0-3.openstacklocal",
                "\"Comment: Command \"/opt/scripts/recipe-runner.sh post-ambari-start failingRecipe1\" run\n"
                        + "Stdout: /opt/scripts/recipe-runner.sh post-ambari-start failingRecipe1 : Timed out after 10 seconds\"");
        return nodesWithErrors;
    }
}