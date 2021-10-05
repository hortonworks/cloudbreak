package com.sequenceiq.sdx.api.model;

import java.util.Set;

public class SdxClusterRequest extends SdxClusterRequestBase {

    private String runtime;

    private Set<SdxRecipe> recipes;

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public Set<SdxRecipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<SdxRecipe> recipes) {
        this.recipes = recipes;
    }

    @Override
    public String toString() {
        return "SdxClusterRequest{" +
                "runtime='" + runtime + '\'' +
                ", recipes=" + recipes +
                "} " + super.toString();
    }
}
