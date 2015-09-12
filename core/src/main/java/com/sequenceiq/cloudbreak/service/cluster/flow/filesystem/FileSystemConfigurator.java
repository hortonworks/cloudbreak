package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem;

import java.util.List;

import com.sequenceiq.cloudbreak.service.cluster.flow.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeScript;

public interface FileSystemConfigurator<T extends FileSystemConfiguration> {

    List<BlueprintConfigurationEntry> getBlueprintProperties(T fsConfig);

    String getDefaultFsValue(T fsConfig);

    List<RecipeScript> getScripts();

    FileSystemType getFileSystemType();
}
