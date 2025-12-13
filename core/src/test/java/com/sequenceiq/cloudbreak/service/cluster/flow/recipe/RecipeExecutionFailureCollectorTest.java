package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ArrayListMultimap;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;

class RecipeExecutionFailureCollectorTest {

    private static final String EXCEPTION_MESSAGE = "\"Comment: Command \"/opt/scripts/recipe-runner.sh post-cloudera-manager-start failingRecipe1\" run\n"
            + "Stdout: /opt/scripts/recipe-runner.sh post-cloudera-manager-start failingRecipe1 : Timed out after 10 seconds\""
            + "\"Comment: Command \"/opt/scripts/recipe-runner.sh pre-service-deployment failingRecipe2\" run\n"
            + "Stdout: /opt/scripts/recipe-runner.sh pre-service-deployment failingRecipe2 : Timed out after 10 seconds\""
            + "Comment: One or more requisite failed: postgresql.init-services-db, postgresql.configure-listen-address"
            + "Comment: One or more requisite failed: postgresql.init-services-db, postgresql.configure-listen-address";

    private final RecipeExecutionFailureCollector recipeExecutionFailureHandler = new RecipeExecutionFailureCollector();

    @Test
    void testCanProcessWithUnprocessableMessage() {
        ArrayListMultimap<String, String> nodesWithErrors = getNodesWithErrors();
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("Something went wrong", nodesWithErrors);
        assertFalse(recipeExecutionFailureHandler.canProcessExecutionFailure(exception));
    }

    @Test
    void testCanProcessProcessableMessage() {
        ArrayListMultimap<String, String> nodesWithErrors = getNodesWithErrors();
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException(EXCEPTION_MESSAGE, nodesWithErrors);
        assertTrue(recipeExecutionFailureHandler.canProcessExecutionFailure(exception));
    }

    @Test
    void testRecipePhaseExtract() {
        String example = "Name: /opt/scripts/recipe-runner.sh pre-service-deployment failing-recipe";
        String result = recipeExecutionFailureHandler.getRecipePhase(example);
        assertEquals("pre-service-deployment", result);
    }

    @Test
    void testRecipeNameExtract() {
        String example = "Name: /opt/scripts/recipe-runner.sh pre-service-deployment failing-recipe";
        String result = recipeExecutionFailureHandler.getFailedRecipeName(example);
        assertEquals("failing-recipe", result);
    }

    @Test
    void testRecipePhaseMultiline() {
        String example = getSingleLineError();
        String result = recipeExecutionFailureHandler.getRecipePhase(example);
        assertEquals("post-cloudera-manager-start", result);
    }

    @Test
    void testCollectErros2() {
        ArrayListMultimap<String, String> nodesWithErrors = getNodesWithErrors();
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException(EXCEPTION_MESSAGE, nodesWithErrors);
        List<RecipeExecutionFailureCollector.RecipeFailure> failure = recipeExecutionFailureHandler.collectErrors(exception);
        assertEquals(3, failure.size());
        assertEquals("failingRecipe1", failure.get(0).getRecipeName());
        assertEquals("post-cloudera-manager-start", failure.get(0).getPhase());

        assertEquals("failingRecipe2", failure.get(1).getRecipeName());
        assertEquals("pre-service-deployment", failure.get(1).getPhase());
    }

    @Test
    void testCollectErrors() {
        ArrayListMultimap<String, String> nodesWithErrors = getNodesWithErrors();
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException(EXCEPTION_MESSAGE, nodesWithErrors);

        List<RecipeExecutionFailureCollector.RecipeFailure> recipeExecutionFailures = recipeExecutionFailureHandler.collectErrors(exception);

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
                .filter(failure -> "host-10-0-0-4.aws".equals(failure.getHost()))
                .count();
        assertEquals(2, workerInstanceFailures);

        long masterInstanceFailures = recipeExecutionFailures.stream()
                .filter(failure -> "host-10-0-0-3.aws".equals(failure.getHost()))
                .peek(failure -> assertEquals("failingRecipe1", failure.getRecipeName()))
                .count();
        assertEquals(1, masterInstanceFailures);
    }

    @Test
    void testGetInstanceMetadataByHostWithNullFqdn() {
        String host = "fqdn";

        final InstanceMetaData im1 = new InstanceMetaData();
        im1.setDiscoveryFQDN(null);
        final InstanceMetaData im2 = new InstanceMetaData();
        im2.setDiscoveryFQDN(host);
        Set<InstanceMetaData> instanceMetaData = Set.of(im1, im2);

        Optional<InstanceMetaData> instanceMetadataByHost = recipeExecutionFailureHandler.getInstanceMetadataByHost(instanceMetaData, host);

        assertTrue(instanceMetadataByHost.isPresent());
        assertEquals(im2, instanceMetadataByHost.get());
    }

    private ArrayListMultimap<String, String> getNodesWithErrors() {
        ArrayListMultimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.putAll("host-10-0-0-4.aws", getMultiLineError());
        nodesWithErrors.put("host-10-0-0-3.aws", getSingleLineError());
        return nodesWithErrors;
    }

    private List<String> getMultiLineError() {
        return Arrays.asList(
                "\"Comment: Command \"/opt/scripts/recipe-runner.sh post-cloudera-manager-start failingRecipe1\" run\n"
                        + "Stdout: /opt/scripts/recipe-runner.sh post-cloudera-manager-start failingRecipe1 : Timed out after 10 seconds\"",
                "\"Comment: Command \"/opt/scripts/recipe-runner.sh pre-service-deployment failingRecipe2\" run\n"
                        + "Stdout: /opt/scripts/recipe-runner.sh pre-service-deployment failingRecipe2 : Timed out after 10 seconds\"",
                "Comment: One or more requisite failed: postgresql.init-services-db, postgresql.configure-listen-address",
                "Comment: One or more requisite failed: postgresql.init-services-db, postgresql.configure-listen-address"
        );
    }

    private String getSingleLineError() {
        return "\"Comment: Command \"/opt/scripts/recipe-runner.sh post-cloudera-manager-start failingRecipe1\" run\n"
                + "Stdout: /opt/scripts/recipe-runner.sh post-cloudera-manager-start failingRecipe1 : Timed out after 10 seconds\"";
    }
}