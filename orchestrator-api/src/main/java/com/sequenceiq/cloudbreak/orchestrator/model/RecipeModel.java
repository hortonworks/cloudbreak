package com.sequenceiq.cloudbreak.orchestrator.model;

import com.sequenceiq.cloudbreak.api.model.RecipeType;

public class RecipeModel {

    private final String name;

    private final RecipeType recipeType;

    private final String script;

    private final String originalScript;

    private final String generatedScript;

    public RecipeModel(String name, RecipeType recipeType, String script, String originalScript, String generatedScript) {
        this.name = name;
        this.recipeType = recipeType;
        this.script = script;
        this.originalScript = originalScript;
        this.generatedScript = generatedScript;
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

    public String getOriginalScript() {
        return originalScript;
    }

    public String getGeneratedScript() {
        return generatedScript;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RecipeModel{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", script=").append(script);
        sb.append(", originalScript=").append(originalScript);
        sb.append(", generatedScript=").append(generatedScript);
        sb.append(", recipeType=").append(recipeType);
        sb.append('}');
        return sb.toString();
    }
}
