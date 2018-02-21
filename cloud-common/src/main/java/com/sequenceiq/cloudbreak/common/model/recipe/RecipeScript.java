package com.sequenceiq.cloudbreak.common.model.recipe;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.RecipeType;

public class RecipeScript {

    private final String script;

    private ExecutionType executionType;

    private final RecipeType recipeType;

    public RecipeScript(String script, RecipeType recipeType) {
        this.script = script;
        this.recipeType = recipeType;
    }

    public RecipeScript(String script, ExecutionType executionType, RecipeType recipeType) {
        this.script = script;
        this.executionType = executionType;
        this.recipeType = recipeType;
    }

    public String getScript() {
        return script;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    public RecipeType getRecipeType() {
        return recipeType;
    }
}
