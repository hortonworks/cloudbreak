package com.sequenceiq.cloudbreak.common.type;

import static com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase.PRE_CLOUDERA_MANAGER_START;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class RecipeExecutionPhaseTest {

    @Test
    public void testOldRecipeExecutionPhaseShouldNotThrowNPE() {
        assertEquals(PRE_CLOUDERA_MANAGER_START, RecipeExecutionPhase.PRE_SERVICE_DEPLOYMENT.oldRecipeExecutionPhase());
        assertEquals(POST_CLUSTER_INSTALL, RecipeExecutionPhase.POST_SERVICE_DEPLOYMENT.oldRecipeExecutionPhase());
        Arrays.stream(RecipeExecutionPhase.values()).forEach(RecipeExecutionPhase::oldRecipeExecutionPhase);
    }

}