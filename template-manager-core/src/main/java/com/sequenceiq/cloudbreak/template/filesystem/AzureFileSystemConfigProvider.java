package com.sequenceiq.cloudbreak.template.filesystem;


import static com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView.TENANT_ID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.wasb.WasbFileSystemConfigurationsView;

@Component
public class AzureFileSystemConfigProvider {

    public static final String CREDENTIAL_SECRET_KEY = "secretKey";

    public static final String ACCESS_KEY = "accessKey";

    public static final String ADLS_TRACKING_CLUSTERNAME_VALUE = "CLOUDBREAK";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFileSystemConfigProvider.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    public BaseFileSystemConfigurationsView decorateFileSystemConfiguration(String uuid, Credential credential, Resource resourceByType,
            BaseFileSystemConfigurationsView fsConfiguration) {
        String resourceGroupName = resourceByType == null ? "" : resourceByType.getResourceName();
        // we have to lookup secret key from the credential because it is not stored in client side
        if (fsConfiguration instanceof AdlsFileSystemConfigurationsView) {
            String adlsTrackingTag = (cbVersion != null) ? ADLS_TRACKING_CLUSTERNAME_VALUE + '-' + cbVersion : ADLS_TRACKING_CLUSTERNAME_VALUE;
            AdlsFileSystemConfigurationsView fileSystemConfigurationsView = (AdlsFileSystemConfigurationsView) fsConfiguration;
            Json attributesFromVault = new Json(credential.getAttributes().getRaw());
            if (StringUtils.isEmpty(fileSystemConfigurationsView.getClientId())) {
                String credentialString = String.valueOf(attributesFromVault.getMap().get(CREDENTIAL_SECRET_KEY));
                String clientId = String.valueOf(attributesFromVault.getMap().get(ACCESS_KEY));
                fileSystemConfigurationsView.setCredential(credentialString);
                fileSystemConfigurationsView.setClientId(clientId);
            }
            if (StringUtils.isEmpty(fileSystemConfigurationsView.getTenantId())) {
                String tenantId = String.valueOf(attributesFromVault.getMap().get(TENANT_ID));
                fileSystemConfigurationsView.setTenantId(tenantId);
            }
            fileSystemConfigurationsView.setAdlsTrackingClusterNameKey(uuid);
            fileSystemConfigurationsView.setAdlsTrackingClusterTypeKey(adlsTrackingTag);
            fileSystemConfigurationsView.setResourceGroupName(resourceGroupName);
        } else if (fsConfiguration instanceof WasbFileSystemConfigurationsView) {
            ((WasbFileSystemConfigurationsView) fsConfiguration).setResourceGroupName(resourceGroupName);
        }
        return fsConfiguration;
    }

}
