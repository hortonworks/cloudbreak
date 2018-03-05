package com.sequenceiq.cloudbreak.blueprint.filesystem;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Resource;

@Component
public class AzureFileSystemConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFileSystemConfigProvider.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    public FileSystemConfiguration decorateFileSystemConfiguration(String uuid, Credential credential, FileSystem fileSystem, Resource resourceByType,
            FileSystemConfiguration fsConfiguration) {

        String resourceGroupName = resourceByType == null ? "" : resourceByType.getResourceName();
        fsConfiguration.addProperty(FileSystemConfiguration.RESOURCE_GROUP_NAME, resourceGroupName);

        if (fsConfiguration instanceof WasbFileSystemConfiguration) {
            Map<String, String> fileSystemProperties = fileSystem.getProperties();
            String secureWasb = fileSystemProperties.getOrDefault("secure", "false");
            fsConfiguration.addProperty("secure", secureWasb);
            }
        // we have to lookup secret key from the credential because it is not stored in client side
        if (fsConfiguration instanceof AdlsFileSystemConfiguration) {

            String adlsTrackingTag = (cbVersion != null) ? AdlsFileSystemConfiguration.ADLS_TRACKING_CLUSTERNAME_VALUE + '-' + cbVersion
                    : AdlsFileSystemConfiguration.ADLS_TRACKING_CLUSTERNAME_VALUE;
            String credentialString = String.valueOf(credential.getAttributes().getMap().get(AdlsFileSystemConfiguration.CREDENTIAL_SECRET_KEY));
            String clientId = String.valueOf(credential.getAttributes().getMap().get(AdlsFileSystemConfiguration.ACCESS_KEY));
            ((AdlsFileSystemConfiguration) fsConfiguration).setCredential(credentialString);
            ((AdlsFileSystemConfiguration) fsConfiguration).setClientId(clientId);
            fsConfiguration.addProperty(AdlsFileSystemConfiguration.ADLS_TRACKING_CLUSTERNAME_KEY, uuid);
            fsConfiguration.addProperty(AdlsFileSystemConfiguration.ADLS_TRACKING_CLUSTERTYPE_KEY, adlsTrackingTag);
        }
            return fsConfiguration;
        }

}
