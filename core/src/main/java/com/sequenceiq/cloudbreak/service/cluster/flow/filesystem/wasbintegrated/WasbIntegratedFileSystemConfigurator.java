package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.wasbintegrated;

import static com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration.STORAGE_CONTAINER;
import static com.sequenceiq.cloudbreak.api.model.FileSystemType.WASB_INTEGRATED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.WasbIntegratedFileSystemConfiguration;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigException;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemScriptConfig;

import groovyx.net.http.HttpResponseException;

@Component
public class WasbIntegratedFileSystemConfigurator extends AbstractFileSystemConfigurator<WasbIntegratedFileSystemConfiguration> {

    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_ATTEMPTS = 50;
    private static final String STORAGE_ACCOUNT_KEY = "storageAccountKey";

    @Inject
    private StorageAccountStatusCheckerTask storageAccountStatusCheckerTask;

    @Inject
    private PollingService<StorageAccountCheckerContext> storagePollingService;

    @Override
    public List<BlueprintConfigurationEntry> getFsProperties(WasbIntegratedFileSystemConfiguration fsConfig, Map<String, String> resourceProperties) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        String storageName = fsConfig.getStorageName();
        String key = resourceProperties.get(STORAGE_ACCOUNT_KEY);

        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.account.key." + storageName + ".blob.core.windows.net", key));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"));
        return bpConfigs;
    }

    @Override
    public String getDefaultFsValue(WasbIntegratedFileSystemConfiguration fsConfig) {
        return "wasb://" + fsConfig.getProperty(STORAGE_CONTAINER) + "@" + fsConfig.getStorageName() + ".blob.core.windows.net";
    }

    @Override
    public FileSystemType getFileSystemType() {
        return WASB_INTEGRATED;
    }

    @Override
    public Map<String, String> createResources(WasbIntegratedFileSystemConfiguration fsConfig) {
        String resourceGroupName = fsConfig.getProperty(FileSystemConfiguration.RESOURCE_GROUP_NAME);
        if (!StringUtils.isEmpty(resourceGroupName)) {
            String tenantId = fsConfig.getTenantId();
            String subscriptionId = fsConfig.getSubscriptionId();
            String appId = fsConfig.getAppId();
            String appPassword = fsConfig.getAppPassword();
            String region = fsConfig.getRegion();
            String storageName = fsConfig.getStorageName();
            AzureRMClient azureClient = new AzureRMClient(tenantId, appId, appPassword, subscriptionId);

            try {
                azureClient.createStorageAccount(resourceGroupName, storageName, region, "Standard_LRS");

                storagePollingService.pollWithTimeoutSingleFailure(storageAccountStatusCheckerTask,
                        new StorageAccountCheckerContext(tenantId, subscriptionId, appId, appPassword, storageName, resourceGroupName),
                        POLLING_INTERVAL, MAX_ATTEMPTS);

                Map<String, Object> keys = azureClient.getStorageAccountKeys(resourceGroupName, storageName);
                String key = (String) keys.get("key1");
                return Collections.singletonMap(STORAGE_ACCOUNT_KEY, key);
            } catch (Exception e) {
                if (e instanceof HttpResponseException) {
                    HttpResponseException he = (HttpResponseException) e;
                    throw new FileSystemConfigException("Failed to create Azure storage resource: " + he.getResponse().getData().toString(), he);
                } else {
                    throw new FileSystemConfigException("Failed to create Azure storage resource", e);
                }

            }
        } else {
            throw new FileSystemConfigException(
                    String.format("The WASB Integrated filesystem is only available on '%s' cloud platform and the '%s' parameter needs to be specified.",
                            CloudConstants.AZURE_RM, FileSystemConfiguration.RESOURCE_GROUP_NAME));
        }
    }

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs() {
        return new ArrayList<>();
    }
}
