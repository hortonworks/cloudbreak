package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeScript;
import com.sequenceiq.cloudbreak.domain.Credential;

public interface FileSystemConfigurator<T extends BaseFileSystemConfigurationsView> {

    Map<String, String> createResources(T fsConfig);

    Map<String, String> deleteResources(T fsConfig);

    List<RecipeScript> getScripts(Credential credential, T fsConfig);

    FileSystemType getFileSystemType();

    String getProtocol();

}
