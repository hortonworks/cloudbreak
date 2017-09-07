package com.sequenceiq.cloudbreak.orchestrator.model;

import com.sequenceiq.cloudbreak.common.type.RecipeType;

public class RecipeModel {

    private final String name;

    private final RecipeType recipeType;

    private final String script;

    public RecipeModel(String name, RecipeType recipeType, String script) {
        this.name = name;
        this.recipeType = recipeType;
        this.script = script;
    }

    public String getName() {
        return name;
    }

    public RecipeType getRecipeType() {
        return recipeType;
    }

    public String getScript() {
        return script;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RecipeModel{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", script=").append(script);
        sb.append(", recipeType=").append(recipeType);
        sb.append('}');
        return sb.toString();
    }
}
