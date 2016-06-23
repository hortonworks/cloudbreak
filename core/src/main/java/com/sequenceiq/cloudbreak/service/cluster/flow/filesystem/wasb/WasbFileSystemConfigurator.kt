package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.wasb

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration.STORAGE_CONTAINER
import com.sequenceiq.cloudbreak.api.model.FileSystemType.WASB

import java.util.ArrayList

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.AbstractFileSystemConfigurator
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemScriptConfig

@Component
class WasbFileSystemConfigurator : AbstractFileSystemConfigurator<WasbFileSystemConfiguration>() {

    override fun getFsProperties(fsConfig: WasbFileSystemConfiguration, resourceProperties: Map<String, String>): List<BlueprintConfigurationEntry> {
        val bpConfigs = ArrayList<BlueprintConfigurationEntry>()
        val accountName = fsConfig.accountName
        val accountKey = fsConfig.accountKey
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.azure.account.key.$accountName.blob.core.windows.net", accountKey))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"))
        return bpConfigs
    }

    override fun getDefaultFsValue(fsConfig: WasbFileSystemConfiguration): String {
        return "wasb://" + fsConfig.getProperty(STORAGE_CONTAINER) + "@" + fsConfig.accountName + ".blob.core.windows.net"
    }

    override val fileSystemType: FileSystemType
        get() = WASB

    override fun getScriptConfigs(fsConfig: WasbFileSystemConfiguration): List<FileSystemScriptConfig> {
        return ArrayList()
    }

}
