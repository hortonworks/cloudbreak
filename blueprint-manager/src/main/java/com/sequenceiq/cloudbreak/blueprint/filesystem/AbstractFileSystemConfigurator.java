package com.sequenceiq.cloudbreak.blueprint.filesystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeScript;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public abstract class AbstractFileSystemConfigurator<T extends FileSystemConfiguration> implements FileSystemConfigurator<T> {

    @Override
    public List<RecipeScript> getScripts(Credential credential, T fsConfig) {
        List<RecipeScript> scripts = new ArrayList<>();
        try {
            for (FileSystemScriptConfig fsScriptConfig : getScriptConfigs(credential, fsConfig)) {
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

    @Override
    public List<BlueprintConfigurationEntry> getDefaultFsProperties(T fsConfig) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        String defaultFs = getDefaultFsValue(fsConfig);
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.defaultFS", defaultFs));
        bpConfigs.add(new BlueprintConfigurationEntry("hbase-site", "hbase.rootdir", defaultFs + "/apps/hbase/data"));
        bpConfigs.add(new BlueprintConfigurationEntry("accumulo-site", "instance.volumes", defaultFs + "/apps/accumulo/data"));
        bpConfigs.add(new BlueprintConfigurationEntry("webhcat-site", "templeton.hive.archive", defaultFs + "/hdp/apps/${hdp.version}/hive/hive.tar.gz"));
        bpConfigs.add(new BlueprintConfigurationEntry("webhcat-site", "templeton.pig.archive", defaultFs + "/hdp/apps/${hdp.version}/pig/pig.tar.gz"));
        bpConfigs.add(new BlueprintConfigurationEntry("webhcat-site", "templeton.sqoop.archive", defaultFs + "/hdp/apps/${hdp.version}/sqoop/sqoop.tar.gz"));
        bpConfigs.add(new BlueprintConfigurationEntry(
                "webhcat-site", "templeton.streaming.jar", defaultFs + "/hdp/apps/${hdp.version}/mapreduce/hadoop-streaming.jar"));
        bpConfigs.add(new BlueprintConfigurationEntry("oozie-site", "oozie.service.HadoopAccessorService.supported.filesystems", "*"));
        return bpConfigs;
    }

    @Override
    public Map<String, String> createResources(T fsConfig) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> deleteResources(T fsConfig) {
        return Collections.emptyMap();
    }

    protected abstract List<FileSystemScriptConfig> getScriptConfigs(Credential credential, T fsConfig);

}
