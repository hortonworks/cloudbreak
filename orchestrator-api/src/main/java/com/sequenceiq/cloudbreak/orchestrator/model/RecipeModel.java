package com.sequenceiq.cloudbreak.orchestrator.model;

import com.sequenceiq.cloudbreak.api.model.RecipeType;

public class RecipeModel {

    private final String name;

    private final RecipeType recipeType;

    private final String generatedScript;

    public RecipeModel(String name, RecipeType recipeType, String generatedScript) {
        this.name = name;
        this.recipeType = recipeType;
        this.generatedScript = generatedScript;
    }

    public String getName() {
        return name;
    }

    public RecipeType getRecipeType() {
        return recipeType;
    }

    public String getGeneratedScript() {
        return generatedScript;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RecipeModel{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", generatedScript=").append(generatedScript);
        sb.append(", recipeType=").append(recipeType);
        sb.append('}');
        return sb.toString();
    }
}
