package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.FileSystemType;
import com.sequenceiq.cloudbreak.domain.Recipe;

public interface FileSystemConfigurator {

    List<BlueprintConfigurationEntry> getBlueprintProperties(Map<String, String> fsProperties);

    String getDefaultFsValue(Map<String, String> fsProperties);

    List<Recipe> getRecipes(Map<String, String> fsProperties);

    FileSystemType getFileSystemType();
}
