package com.sequenceiq.cloudbreak.common.model.recipe;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RecipeScript that = (RecipeScript) o;

        return new EqualsBuilder()
                .append(script, that.script)
                .append(executionType, that.executionType)
                .append(recipeType, that.recipeType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(script)
                .append(executionType)
                .append(recipeType)
                .toHashCode();
    }
}
