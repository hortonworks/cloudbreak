package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem

import java.io.IOException
import java.util.ArrayList
import java.util.Collections

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeScript
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry
import com.sequenceiq.cloudbreak.util.FileReaderUtils

abstract class AbstractFileSystemConfigurator<T : FileSystemConfiguration> : FileSystemConfigurator<T> {

    override fun getScripts(fsConfig: T): List<RecipeScript> {
        val scripts = ArrayList<RecipeScript>()
        try {
            for (fsScriptConfig in getScriptConfigs(fsConfig)) {
                var script = FileReaderUtils.readFileFromClasspath(fsScriptConfig.scriptLocation)
                for (key in fsScriptConfig.properties.keys) {
                    script = script.replace(("\\$" + key).toRegex(), fsScriptConfig.properties[key])
                }
                scripts.add(RecipeScript(script, fsScriptConfig.clusterLifecycleEvent, fsScriptConfig.executionType))
            }
        } catch (e: IOException) {
            throw FileSystemConfigException("Filesystem configuration scripts cannot be read.", e)
        }

        return scripts
    }

    override fun getDefaultFsProperties(fsConfig: T): List<BlueprintConfigurationEntry> {
        val bpConfigs = ArrayList<BlueprintConfigurationEntry>()
        val defaultFs = getDefaultFsValue(fsConfig)
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.defaultFS", defaultFs))
        bpConfigs.add(BlueprintConfigurationEntry("hbase-site", "hbase.rootdir", defaultFs + "/apps/hbase/data"))
        bpConfigs.add(BlueprintConfigurationEntry("accumulo-site", "instance.volumes", defaultFs + "/apps/accumulo/data"))
        bpConfigs.add(BlueprintConfigurationEntry("webhcat-site", "templeton.hive.archive", defaultFs + "/hdp/apps/${hdp.version}/hive/hive.tar.gz"))
        bpConfigs.add(BlueprintConfigurationEntry("webhcat-site", "templeton.pig.archive", defaultFs + "/hdp/apps/${hdp.version}/pig/pig.tar.gz"))
        bpConfigs.add(BlueprintConfigurationEntry("webhcat-site", "templeton.sqoop.archive", defaultFs + "/hdp/apps/${hdp.version}/sqoop/sqoop.tar.gz"))
        bpConfigs.add(BlueprintConfigurationEntry(
                "webhcat-site", "templeton.streaming.jar", defaultFs + "/hdp/apps/${hdp.version}/mapreduce/hadoop-streaming.jar"))
        bpConfigs.add(BlueprintConfigurationEntry("oozie-site", "oozie.service.HadoopAccessorService.supported.filesystems", "*"))
        return bpConfigs
    }

    override fun createResources(fsConfig: T): Map<String, String> {
        return emptyMap<String, String>()
    }

    override fun deleteResources(fsConfig: T): Map<String, String> {
        return emptyMap<String, String>()
    }

    protected abstract fun getScriptConfigs(fsConfig: T): List<FileSystemScriptConfig>

}
