package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;

public class FileSystemScriptConfig {
    private final String scriptLocation;

    private final RecipeType recipeType;

    private final ExecutionType executionType;

    private final Map<String, String> properties;

    public FileSystemScriptConfig(String scriptLocation, RecipeType recipeType, ExecutionType executionType) {
        this(scriptLocation, recipeType, executionType, Maps.newHashMap());
    }

    public FileSystemScriptConfig(String scriptLocation, RecipeType recipeType, ExecutionType executionType, Map<String, String> properties) {
        this.scriptLocation = scriptLocation;
        this.recipeType = recipeType;
        this.executionType = executionType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileSystemScriptConfig that = (FileSystemScriptConfig) o;

        return new EqualsBuilder()
                .append(scriptLocation, that.scriptLocation)
                .append(recipeType, that.recipeType)
                .append(executionType, that.executionType)
                .append(properties, that.properties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(scriptLocation)
                .append(recipeType)
                .append(executionType)
                .append(properties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "FileSystemScriptConfig{"
                + "scriptLocation='" + scriptLocation + '\''
                + ", recipeType=" + recipeType
                + ", executionType=" + executionType
                + ", properties=" + properties
                + '}';
    }
}
