package com.sequenceiq.cloudbreak.common.type;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;

public enum RecipeExecutionPhase {

    @Deprecated PRE_CLOUDERA_MANAGER_START("pre-cloudera-manager-start"),
    PRE_SERVICE_DEPLOYMENT("pre-service-deployment", PRE_CLOUDERA_MANAGER_START),
    PRE_TERMINATION("pre-termination"),
    POST_CLOUDERA_MANAGER_START("post-cloudera-manager-start"),
    @Deprecated POST_CLUSTER_INSTALL("post-cluster-install"),
    POST_SERVICE_DEPLOYMENT("post-service-deployment", POST_CLUSTER_INSTALL);

    private final String value;

    private final RecipeExecutionPhase oldRecipeExecutionPhase;

    RecipeExecutionPhase(String value) {
        this.value = value;
        this.oldRecipeExecutionPhase = null;
    }

    RecipeExecutionPhase(String value, RecipeExecutionPhase oldRecipeExecutionPhase) {
        this.value = value;
        this.oldRecipeExecutionPhase = oldRecipeExecutionPhase;
    }

    public static RecipeExecutionPhase convert(RecipeType recipeType) {
        switch (recipeType) {
            case PRE_CLOUDERA_MANAGER_START:
            case PRE_SERVICE_DEPLOYMENT:
                return PRE_SERVICE_DEPLOYMENT;
            case PRE_TERMINATION:
                return PRE_TERMINATION;
            case POST_CLOUDERA_MANAGER_START:
                return POST_CLOUDERA_MANAGER_START;
            case POST_CLUSTER_INSTALL:
            case POST_SERVICE_DEPLOYMENT:
                return POST_SERVICE_DEPLOYMENT;
            default:
                throw new UnsupportedOperationException("Unsupported Execution Phase: " + recipeType);
        }
    }

    public RecipeExecutionPhase oldRecipeExecutionPhase() {
        return oldRecipeExecutionPhase;
    }

    public boolean isPreRecipe() {
        return this != POST_SERVICE_DEPLOYMENT && this != POST_CLUSTER_INSTALL;
    }

    public boolean isPostRecipe() {
        return !isPreRecipe();
    }

    public String value() {
        return value;
    }

}
