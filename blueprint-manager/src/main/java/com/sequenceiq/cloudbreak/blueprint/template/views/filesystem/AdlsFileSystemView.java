package com.sequenceiq.cloudbreak.blueprint.template.views.filesystem;

import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;

public class AdlsFileSystemView extends FileSystemView<AdlsFileSystemConfiguration> {

    private final String accountName;

    private final String clientId;

    private final String credential;

    private final String tenantId;

    public AdlsFileSystemView(FileSystemConfigurationView fileSystemConfigurationView) {
        super(fileSystemConfigurationView);
        AdlsFileSystemConfiguration adlsConfig = (AdlsFileSystemConfiguration) fileSystemConfigurationView.getFileSystemConfiguration();
        accountName = adlsConfig.getAccountName();
        clientId = adlsConfig.getClientId();
        credential = adlsConfig.getCredential();
        tenantId = adlsConfig.getTenantId();
    }

    @Override
    public String defaultFsValue(AdlsFileSystemConfiguration fileSystemConfiguration) {
        return "adl://" + fileSystemConfiguration.getAccountName() + ".azuredatalakestore.net";
    }

    public String getAccountName() {
        return accountName;
    }

    public String getClientId() {
        return clientId;
    }

    public String getCredential() {
        return credential;
    }

    public String getTenantId() {
        return tenantId;
    }

}
