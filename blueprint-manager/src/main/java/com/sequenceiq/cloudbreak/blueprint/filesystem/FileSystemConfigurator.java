package com.sequenceiq.cloudbreak.blueprint.filesystem;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeScript;
import com.sequenceiq.cloudbreak.domain.Credential;

public interface FileSystemConfigurator<T extends FileSystemConfiguration> {

    Map<String, String> createResources(T fsConfig);

    Map<String, String> deleteResources(T fsConfig);

    List<BlueprintConfigurationEntry> getFsProperties(T fsConfig, Map<String, String> resourceProperties);

    String getDefaultFsValue(T fsConfig);

    List<BlueprintConfigurationEntry> getDefaultFsProperties(T fsConfig);

    List<RecipeScript> getScripts(Credential credential, T fsConfig);

    FileSystemType getFileSystemType();

}
