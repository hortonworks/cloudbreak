package com.sequenceiq.cloudbreak.blueprint.template.views;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.GcsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.WasbIntegratedFileSystemConfiguration;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class FileSystemView {

    private Map<String, String> properties = new HashMap<>();

    public FileSystemView(FileSystemConfiguration fileSystemConfiguration) {
        if (fileSystemConfiguration != null) {
            if (fileSystemConfiguration instanceof WasbIntegratedFileSystemConfiguration) {
                WasbIntegratedFileSystemConfiguration wasbIntegratedConfig = (WasbIntegratedFileSystemConfiguration) fileSystemConfiguration;
                properties.put("appId", wasbIntegratedConfig.getAppId());
                properties.put("appPassword", wasbIntegratedConfig.getAppPassword());
                properties.put("region", wasbIntegratedConfig.getRegion());
                properties.put("storageName", wasbIntegratedConfig.getStorageName());
                properties.put("subscriptionId", wasbIntegratedConfig.getSubscriptionId());
                properties.put("tenantId", wasbIntegratedConfig.getTenantId());
            } else if (fileSystemConfiguration instanceof GcsFileSystemConfiguration) {
                GcsFileSystemConfiguration gcsConfig = (GcsFileSystemConfiguration) fileSystemConfiguration;
                properties.put("defaultBucketName", gcsConfig.getDefaultBucketName());
                properties.put("projectId", gcsConfig.getProjectId());
                properties.put("serviceAccountEmail", gcsConfig.getServiceAccountEmail());
            } else if (fileSystemConfiguration instanceof WasbFileSystemConfiguration) {
                WasbFileSystemConfiguration wasbConfig = (WasbFileSystemConfiguration) fileSystemConfiguration;
                properties.put("accountKey", wasbConfig.getAccountKey());
                properties.put("accountName", wasbConfig.getAccountName());
            } else if (fileSystemConfiguration instanceof AdlsFileSystemConfiguration) {
                AdlsFileSystemConfiguration adlsConfig = (AdlsFileSystemConfiguration) fileSystemConfiguration;
                properties.put("accountName", adlsConfig.getAccountName());
                properties.put("clientId", adlsConfig.getClientId());
                properties.put("credential", adlsConfig.getCredential());
                properties.put("tenantId", adlsConfig.getTenantId());
            } else {
                String message = String.format("Could not cast FileSystem '%s' to FileSystemView because the object class was not implemented: %s",
                        fileSystemConfiguration, fileSystemConfiguration.getClass());
                throw new CloudbreakServiceException(message);
            }
            properties.putAll(fileSystemConfiguration.getDynamicProperties());
        }
    }

    public Map<String, String> getProperties() {
        return properties;
    }

}
