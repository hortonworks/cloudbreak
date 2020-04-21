package com.sequenceiq.cloudbreak.common.type;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;

public enum RecipeExecutionPhase {

    PRE_CLOUDERA_MANAGER_START("pre-cloudera-manager-start"),
    PRE_TERMINATION("pre-termination"),
    POST_CLOUDERA_MANAGER_START("post-cloudera-manager-start"),
    POST_CLUSTER_INSTALL("post-cluster-install");

    private final String value;

    RecipeExecutionPhase(String value) {
        this.value = value;
    }

    public static RecipeExecutionPhase convert(RecipeType recipeType) {
        switch (recipeType) {
            case PRE_CLOUDERA_MANAGER_START:
                return PRE_CLOUDERA_MANAGER_START;
            case PRE_TERMINATION:
                return PRE_TERMINATION;
            case POST_CLOUDERA_MANAGER_START:
                return POST_CLOUDERA_MANAGER_START;
            case POST_CLUSTER_INSTALL:
                return POST_CLUSTER_INSTALL;
            default:
                throw new UnsupportedOperationException("Unsupported Execution Phase: " + recipeType);
        }
    }

    public boolean isPreRecipe() {
        return this != POST_CLUSTER_INSTALL;
    }

    public boolean isPostRecipe() {
        return !isPreRecipe();
    }

    public String value() {
        return value;
    }

}
