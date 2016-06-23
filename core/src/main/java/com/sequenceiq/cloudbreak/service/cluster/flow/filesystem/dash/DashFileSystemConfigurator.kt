package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.dash

import com.sequenceiq.cloudbreak.api.model.ExecutionType.ALL_NODES
import com.sequenceiq.cloudbreak.api.model.ExecutionType.ONE_NODE
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration.STORAGE_CONTAINER
import com.sequenceiq.cloudbreak.api.model.FileSystemType.DASH
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterLifecycleEvent.POST_INSTALL

import java.util.ArrayList

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.DashFileSystemConfiguration
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.AbstractFileSystemConfigurator
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemScriptConfig

@Component
class DashFileSystemConfigurator : AbstractFileSystemConfigurator<DashFileSystemConfiguration>() {

    override fun getFsProperties(fsConfig: DashFileSystemConfiguration, resourceProperties: Map<String, String>): List<BlueprintConfigurationEntry> {
        val bpConfigs = ArrayList<BlueprintConfigurationEntry>()
        val dashAccountName = fsConfig.accountName
        val dashAccountKey = fsConfig.accountKey
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.azure.account.key.$dashAccountName.cloudapp.net", dashAccountKey))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"))
        return bpConfigs
    }

    override fun getDefaultFsValue(fsConfig: DashFileSystemConfiguration): String {
        return "wasb://" + fsConfig.getProperty(STORAGE_CONTAINER) + "@" + fsConfig.accountName + ".cloudapp.net"
    }

    override val fileSystemType: FileSystemType
        get() = DASH

    override fun getScriptConfigs(fsConfig: DashFileSystemConfiguration): List<FileSystemScriptConfig> {
        val scriptConfigs = ArrayList<FileSystemScriptConfig>()
        scriptConfigs.add(FileSystemScriptConfig("scripts/dash-local.sh", POST_INSTALL, ALL_NODES))
        scriptConfigs.add(FileSystemScriptConfig("scripts/dash-hdfs.sh", POST_INSTALL, ONE_NODE))
        return scriptConfigs
    }
}
