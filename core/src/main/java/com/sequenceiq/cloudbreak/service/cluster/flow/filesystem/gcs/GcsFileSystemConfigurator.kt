package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.gcs

import com.sequenceiq.cloudbreak.api.model.ExecutionType.ALL_NODES
import com.sequenceiq.cloudbreak.api.model.FileSystemType.GCS
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterLifecycleEvent.PRE_INSTALL

import java.util.ArrayList
import java.util.Collections

import org.apache.commons.codec.binary.Base64
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.api.model.GcsFileSystemConfiguration
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.AbstractFileSystemConfigurator
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemScriptConfig

@Component
class GcsFileSystemConfigurator : AbstractFileSystemConfigurator<GcsFileSystemConfiguration>() {

    override fun getScriptConfigs(fsConfig: GcsFileSystemConfiguration): List<FileSystemScriptConfig> {
        val privateKey = getPrivateKey(fsConfig)
        val properties = Collections.singletonMap("P12KEY", privateKey)
        val fsScriptConfigs = ArrayList<FileSystemScriptConfig>()
        fsScriptConfigs.add(FileSystemScriptConfig("scripts/gcs-connector.sh", PRE_INSTALL, ALL_NODES))
        fsScriptConfigs.add(FileSystemScriptConfig("scripts/gcs-p12.sh", PRE_INSTALL, ALL_NODES, properties))
        return fsScriptConfigs
    }

    private fun getPrivateKey(fsConfig: GcsFileSystemConfiguration): String {
        val privateKeyEncoded = fsConfig.privateKeyEncoded
        return Base64.encodeBase64String(privateKeyEncoded.toByteArray())
    }

    override fun getFsProperties(fsConfig: GcsFileSystemConfiguration, resourceProperties: Map<String, String>): List<BlueprintConfigurationEntry> {
        val bpConfigs = ArrayList<BlueprintConfigurationEntry>()
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.gs.working.dir", "/"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.gs.system.bucket", fsConfig.defaultBucketName))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.gs.auth.service.account.enable", "true"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.gs.auth.service.account.keyfile", "/usr/lib/hadoop/lib/gcp.p12"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.gs.auth.service.account.email", fsConfig.serviceAccountEmail))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.gs.project.id", fsConfig.projectId))
        return bpConfigs
    }

    override fun getDefaultFsValue(fsConfig: GcsFileSystemConfiguration): String {
        return String.format("gs://%s/", fsConfig.defaultBucketName)
    }

    override val fileSystemType: FileSystemType
        get() = GCS
}
