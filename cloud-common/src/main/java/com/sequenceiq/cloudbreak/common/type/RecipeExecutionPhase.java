package com.sequenceiq.cloudbreak.common.type;

public enum RecipeExecutionPhase {
    PRE("pre"), POST("post");

    private String value;

    RecipeExecutionPhase(String value) {
        this.value = value;
    }

    public static RecipeExecutionPhase convert(RecipeType recipeType) {
        switch (recipeType) {
            case PRE:
                return PRE;
            case POST:
                return POST;
            default:
                throw new UnsupportedOperationException("Unsupported Execution Phase: " + recipeType);
        }
    }

    public String value() {
        return value;
    }

}
