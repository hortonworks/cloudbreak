package com.sequenceiq.cloudbreak.template.filesystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeScript;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public abstract class AbstractFileSystemConfigurator<T extends BaseFileSystemConfigurationsView> implements FileSystemConfigurator<T> {

    @Override
    public List<RecipeScript> getScripts(Credential credential, T fsConfig) {
        List<RecipeScript> scripts = new ArrayList<>();
        try {
            for (FileSystemScriptConfig fsScriptConfig : getScriptConfigs(credential)) {
                String script = FileReaderUtils.readFileFromClasspath(fsScriptConfig.getScriptLocation());
                for (Entry<String, String> entry : fsScriptConfig.getProperties().entrySet()) {
                    script = script.replaceAll("\\$" + entry.getKey(), entry.getValue());
                }
                scripts.add(new RecipeScript(script, fsScriptConfig.getExecutionType(), fsScriptConfig.getRecipeType()));
            }
        } catch (IOException e) {
            throw new FileSystemConfigException("Filesystem configuration scripts cannot be read.", e);
        }
        return scripts;
    }

    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> createResources(T fsConfig) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> deleteResources(T fsConfig) {
        return Collections.emptyMap();
    }

}
