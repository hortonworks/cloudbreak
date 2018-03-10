package com.sequenceiq.cloudbreak.blueprint.filesystem;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.RecipeType;

public class FileSystemScriptConfig {
    private final String scriptLocation;

    private final RecipeType recipeType;

    private final ExecutionType executionType;

    private Map<String, String> properties = new HashMap<>();

    public FileSystemScriptConfig(String scriptLocation, RecipeType recipeType, ExecutionType executionType) {
        this.scriptLocation = scriptLocation;
        this.recipeType = recipeType;
        this.executionType = executionType;
    }

    public FileSystemScriptConfig(String scriptLocation, RecipeType recipeType, ExecutionType executionType, Map<String, String> properties) {
        this(scriptLocation, recipeType, executionType);
        this.properties = properties;
    }

    public String getScriptLocation() {
        return scriptLocation;
    }

    public RecipeType getRecipeType() {
        return recipeType;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
