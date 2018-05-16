package com.sequenceiq.cloudbreak.blueprint.template.views.filesystem;

import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;

public class AdlsFileSystemView extends FileSystemView<AdlsFileSystemConfiguration> {

    private String accountName;

    private String clientId;

    private String credential;

    private String tenantId;

    public AdlsFileSystemView(FileSystemConfigurationView fileSystemConfigurationView) {
        super(fileSystemConfigurationView);
        AdlsFileSystemConfiguration adlsConfig = (AdlsFileSystemConfiguration) fileSystemConfigurationView.getFileSystemConfiguration();
        this.accountName = adlsConfig.getAccountName();
        this.clientId = adlsConfig.getClientId();
        this.credential = adlsConfig.getCredential();
        this.tenantId = adlsConfig.getTenantId();
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
