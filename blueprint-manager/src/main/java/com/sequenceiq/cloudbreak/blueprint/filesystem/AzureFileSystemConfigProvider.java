package com.sequenceiq.cloudbreak.blueprint.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.blueprint.filesystem.wasb.WasbFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;

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
            String credentialString = String.valueOf(credential.getAttributes().getMap().get(CREDENTIAL_SECRET_KEY));
            String clientId = String.valueOf(credential.getAttributes().getMap().get(ACCESS_KEY));
            ((AdlsFileSystemConfigurationsView) fsConfiguration).setCredential(credentialString);
            ((AdlsFileSystemConfigurationsView) fsConfiguration).setClientId(clientId);
            ((AdlsFileSystemConfigurationsView) fsConfiguration).setAdlsTrackingClusterNameKey(uuid);
            ((AdlsFileSystemConfigurationsView) fsConfiguration).setAdlsTrackingClusterTypeKey(adlsTrackingTag);
            ((AdlsFileSystemConfigurationsView) fsConfiguration).setResourceGroupName(resourceGroupName);
        } else if (fsConfiguration instanceof WasbFileSystemConfigurationsView) {
            ((WasbFileSystemConfigurationsView) fsConfiguration).setResourceGroupName(resourceGroupName);
        }
        return fsConfiguration;
    }

}
