package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.wasb;

import static com.sequenceiq.cloudbreak.model.FileSystemConfiguration.STORAGE_CONTAINER;
import static com.sequenceiq.cloudbreak.model.FileSystemType.WASB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.service.cluster.flow.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemScriptConfig;
import com.sequenceiq.cloudbreak.model.FileSystemType;
import org.springframework.stereotype.Component;

@Component
public class WasbFileSystemConfigurator extends AbstractFileSystemConfigurator<WasbFileSystemConfiguration> {

    @Override
    public List<BlueprintConfigurationEntry> getFsProperties(WasbFileSystemConfiguration fsConfig, Map<String, String> resourceProperties) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        String accountName = fsConfig.getAccountName();
        String accountKey = fsConfig.getAccountKey();
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.account.key." + accountName + ".blob.core.windows.net", accountKey));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"));
        return bpConfigs;
    }

    @Override
    public String getDefaultFsValue(WasbFileSystemConfiguration fsConfig) {
        return "wasb://" + fsConfig.getProperty(STORAGE_CONTAINER) + "@" + fsConfig.getAccountName() + ".blob.core.windows.net";
    }

    @Override
    public FileSystemType getFileSystemType() {
        return WASB;
    }

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs() {
        return new ArrayList<>();
    }

}
