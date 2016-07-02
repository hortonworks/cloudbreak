package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeScript;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry;

public interface FileSystemConfigurator<T extends FileSystemConfiguration> {

    Map<String, String> createResources(T fsConfig);

    Map<String, String> deleteResources(T fsConfig);

    List<BlueprintConfigurationEntry> getFsProperties(T fsConfig, Map<String, String> resourceProperties);

    String getDefaultFsValue(T fsConfig);

    List<BlueprintConfigurationEntry> getDefaultFsProperties(T fsConfig);

    List<RecipeScript> getScripts(T fsConfig);

    FileSystemType getFileSystemType();

}
