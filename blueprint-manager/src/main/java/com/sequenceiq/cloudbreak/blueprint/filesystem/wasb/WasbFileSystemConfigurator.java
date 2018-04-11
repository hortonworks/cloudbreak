package com.sequenceiq.cloudbreak.blueprint.filesystem.wasb;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemScriptConfig;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateConfigurationEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration.STORAGE_CONTAINER;
import static com.sequenceiq.cloudbreak.api.model.FileSystemType.WASB;

@Component
public class WasbFileSystemConfigurator extends AbstractFileSystemConfigurator<WasbFileSystemConfiguration> {

    @Override
    public List<TemplateConfigurationEntry> getFsProperties(WasbFileSystemConfiguration fsConfig, Map<String, String> resourceProperties) {
        List<TemplateConfigurationEntry> bpConfigs = new ArrayList<>();
        String accountName = fsConfig.getAccountName();
        String accountKey = fsConfig.getAccountKey();
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "fs.AbstractFileSystem.wasbs.impl", "org.apache.hadoop.fs.azure.Wasbs"));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "fs.azure.account.key." + accountName + ".blob.core.windows.net", accountKey));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"));
        bpConfigs.add(new TemplateConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"));
        return bpConfigs;
    }

    @Override
    public String getDefaultFsValue(WasbFileSystemConfiguration fsConfig) {
        String protocol = fsConfig.isSecure() ? "wasbs://" : "wasb://";
        return protocol + fsConfig.getProperty(STORAGE_CONTAINER) + '@' + fsConfig.getAccountName() + ".blob.core.windows.net";
    }

    @Override
    public FileSystemType getFileSystemType() {
        return WASB;
    }

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential, WasbFileSystemConfiguration fsConfig) {
        return new ArrayList<>();
    }

}
