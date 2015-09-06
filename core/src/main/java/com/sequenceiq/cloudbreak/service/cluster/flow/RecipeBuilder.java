package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.Recipe;

public interface RecipeBuilder {

    List<Recipe> buildRecipes(List<FileSystemScript> fileSystemScripts, Map<String, String> properties)

}
