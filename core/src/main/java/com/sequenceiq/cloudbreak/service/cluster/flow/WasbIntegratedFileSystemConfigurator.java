package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.domain.FileSystemType.WASB_INTEGRATED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.domain.FileSystemType;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.FileSystemConfigException;

import groovyx.net.http.HttpResponseException;

@Component
public class WasbIntegratedFileSystemConfigurator extends AbstractFileSystemConfigurator {

    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_ATTEMPTS = 50;

    @Inject
    private StorageAccountStatusCheckerTask storageAccountStatusCheckerTask;

    @Inject
    private PollingService<StorageAccountCheckerContext> storagePollingService;

    @Override
    public List<BlueprintConfigurationEntry> getBlueprintProperties(Map<String, String> fsProperties) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();

        String tenantId = fsProperties.get("tenant.id");
        String subscriptionId = fsProperties.get("subscription.id");
        String appId = fsProperties.get("app.id");
        String appPassword = fsProperties.get("app.password");
        String region = fsProperties.get("storage.region");
        String storageName = fsProperties.get("storage.name");

        AzureRMClient azureClient = new AzureRMClient(tenantId, appId, appPassword, subscriptionId);
        try {
            azureClient.createResourceGroup(storageName, region);
            azureClient.createStorageAccount(storageName, storageName, region, "Standard_LRS");

            storagePollingService.pollWithTimeout(storageAccountStatusCheckerTask,
                    new StorageAccountCheckerContext(tenantId, subscriptionId, appId, appPassword, storageName, storageName), POLLING_INTERVAL, MAX_ATTEMPTS);

            Map<String, Object> keys = azureClient.getStorageAccountKeys(storageName, storageName);
            String key = (String) keys.get("key1");

            bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
            bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.account.key." + storageName + ".blob.core.windows.net", key));
            bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"));
            bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"));
            return bpConfigs;
        } catch (Exception e) {
            if (e instanceof HttpResponseException) {
                HttpResponseException he = (HttpResponseException) e;
                throw new FileSystemConfigException("Failed to create Azure storage resource: " + he.getResponse().getData().toString(), he);
            } else {
                throw new FileSystemConfigException("Failed to create Azure storage resource", e);
            }

        }
    }

    @Override
    public String getDefaultFsValue(Map<String, String> fsProperties) {
        String storageName = fsProperties.get("storage.name");
        return "wasb://cloudbreak@" + storageName + ".blob.core.windows.net";
    }

    @Override
    public FileSystemType getFileSystemType() {
        return WASB_INTEGRATED;
    }

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs() {
        return new ArrayList<>();
    }
}
