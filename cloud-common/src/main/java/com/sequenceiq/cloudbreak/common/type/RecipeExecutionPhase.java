package com.sequenceiq.cloudbreak.common.type;

public enum RecipeExecutionPhase {

    PRE("pre"),
    POST("post"),
    PRE_AMBARI_START("pre-ambari-start"),
    POST_AMBARI_START("post-ambari-start"),
    POST_CLUSTER_INSTALL("post-cluster-install");

    private final String value;

    RecipeExecutionPhase(String value) {
        this.value = value;
    }

    public static RecipeExecutionPhase convert(RecipeType recipeType) {
        switch (recipeType) {
            case PRE_AMBARI_START:
                return PRE_AMBARI_START;
            case POST_AMBARI_START:
                return POST_AMBARI_START;
            case POST_CLUSTER_INSTALL:
                return POST_CLUSTER_INSTALL;
            default:
                throw new UnsupportedOperationException("Unsupported Execution Phase: " + recipeType);
        }
    }

    public boolean isPreRecipe() {
        switch (this) {
            case POST_CLUSTER_INSTALL:
            case POST:
                return false;
            default:
                return true;
        }
    }

    public boolean isPostRecipe() {
        return !isPreRecipe();
    }

    public String value() {
        return value;
    }

}
