package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.wasbintegrated

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration.STORAGE_CONTAINER
import com.sequenceiq.cloudbreak.api.model.FileSystemType.WASB_INTEGRATED

import java.util.ArrayList
import java.util.Collections

import javax.inject.Inject

import org.springframework.stereotype.Component
import org.springframework.util.StringUtils

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.common.type.CloudConstants
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.api.model.WasbIntegratedFileSystemConfiguration
import com.sequenceiq.cloudbreak.service.PollingService
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.AbstractFileSystemConfigurator
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigException
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemScriptConfig

import groovyx.net.http.HttpResponseException

@Component
class WasbIntegratedFileSystemConfigurator : AbstractFileSystemConfigurator<WasbIntegratedFileSystemConfiguration>() {

    @Inject
    private val storageAccountStatusCheckerTask: StorageAccountStatusCheckerTask? = null

    @Inject
    private val storagePollingService: PollingService<StorageAccountCheckerContext>? = null

    override fun getFsProperties(fsConfig: WasbIntegratedFileSystemConfiguration, resourceProperties: Map<String, String>): List<BlueprintConfigurationEntry> {
        val bpConfigs = ArrayList<BlueprintConfigurationEntry>()
        val storageName = fsConfig.storageName
        val key = resourceProperties[STORAGE_ACCOUNT_KEY]

        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.azure.account.key.$storageName.blob.core.windows.net", key))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"))
        bpConfigs.add(BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"))
        return bpConfigs
    }

    override fun getDefaultFsValue(fsConfig: WasbIntegratedFileSystemConfiguration): String {
        return "wasb://" + fsConfig.getProperty(STORAGE_CONTAINER) + "@" + fsConfig.storageName + ".blob.core.windows.net"
    }

    override val fileSystemType: FileSystemType
        get() = WASB_INTEGRATED

    override fun createResources(fsConfig: WasbIntegratedFileSystemConfiguration): Map<String, String> {
        val resourceGroupName = fsConfig.getProperty(FileSystemConfiguration.RESOURCE_GROUP_NAME)
        if (!StringUtils.isEmpty(resourceGroupName)) {
            val tenantId = fsConfig.tenantId
            val subscriptionId = fsConfig.subscriptionId
            val appId = fsConfig.appId
            val appPassword = fsConfig.appPassword
            val region = fsConfig.region
            val storageName = fsConfig.storageName
            val azureClient = AzureRMClient(tenantId, appId, appPassword, subscriptionId)

            try {
                azureClient.createStorageAccount(resourceGroupName, storageName, region, "Standard_LRS")

                storagePollingService!!.pollWithTimeoutSingleFailure(storageAccountStatusCheckerTask,
                        StorageAccountCheckerContext(tenantId, subscriptionId, appId, appPassword, storageName, resourceGroupName),
                        POLLING_INTERVAL, MAX_ATTEMPTS)

                val keys = azureClient.getStorageAccountKeys(resourceGroupName, storageName)
                val key = keys["key1"] as String
                return Collections.singletonMap(STORAGE_ACCOUNT_KEY, key)
            } catch (e: Exception) {
                if (e is HttpResponseException) {
                    throw FileSystemConfigException("Failed to create Azure storage resource: " + e.response.data.toString(), e)
                } else {
                    throw FileSystemConfigException("Failed to create Azure storage resource", e)
                }

            }

        } else {
            throw FileSystemConfigException(
                    String.format("The WASB Integrated filesystem is only available on '%s' cloud platform and the '%s' parameter needs to be specified.",
                            CloudConstants.AZURE_RM, FileSystemConfiguration.RESOURCE_GROUP_NAME))
        }
    }

    override fun getScriptConfigs(fsConfig: WasbIntegratedFileSystemConfiguration): List<FileSystemScriptConfig> {
        return ArrayList()
    }

    companion object {

        private val POLLING_INTERVAL = 5000
        private val MAX_ATTEMPTS = 50
        private val STORAGE_ACCOUNT_KEY = "storageAccountKey"
    }
}
