package com.sequenceiq.cloudbreak.blueprint.template.views.filesystem;

import static com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration.STORAGE_CONTAINER;

import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;

public class WasbFileSystemView extends FileSystemView<WasbFileSystemConfiguration> {

    private final String accountKey;

    private final String accountName;

    private final boolean secure;

    public WasbFileSystemView(FileSystemConfigurationView fileSystemConfigurationView) {
        super(fileSystemConfigurationView);
        WasbFileSystemConfiguration wasbConfig = (WasbFileSystemConfiguration) fileSystemConfigurationView.getFileSystemConfiguration();
        secure = wasbConfig.isSecure();
        accountKey = wasbConfig.getAccountKey();
        accountName = wasbConfig.getAccountName();
    }

    @Override
    public String defaultFsValue(WasbFileSystemConfiguration fileSystemConfiguration) {
        String protocol = fileSystemConfiguration.isSecure() ? "wasbs://" : "wasb://";
        return protocol + fileSystemConfiguration.getProperty(STORAGE_CONTAINER) + '@' + fileSystemConfiguration.getAccountName() + ".blob.core.windows.net";
    }

    public String getAccountKey() {
        return accountKey;
    }

    public String getAccountName() {
        return accountName;
    }

    public boolean isSecure() {
        return secure;
    }
}
