package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.dash;

import static com.sequenceiq.cloudbreak.common.type.PluginExecutionType.ALL_NODES;
import static com.sequenceiq.cloudbreak.common.type.PluginExecutionType.ONE_NODE;
import static com.sequenceiq.cloudbreak.model.FileSystemConfiguration.STORAGE_CONTAINER;
import static com.sequenceiq.cloudbreak.model.FileSystemType.DASH;
import static com.sequenceiq.cloudbreak.service.cluster.flow.ClusterLifecycleEvent.POST_INSTALL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.model.DashFileSystemConfiguration;
import com.sequenceiq.cloudbreak.model.FileSystemType;
import com.sequenceiq.cloudbreak.service.cluster.flow.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemScriptConfig;

@Component
public class DashFileSystemConfigurator extends AbstractFileSystemConfigurator<DashFileSystemConfiguration> {

    @Override
    public List<BlueprintConfigurationEntry> getFsProperties(DashFileSystemConfiguration fsConfig, Map<String, String> resourceProperties) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        String dashAccountName = fsConfig.getAccountName();
        String dashAccountKey = fsConfig.getAccountKey();
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.account.key." + dashAccountName + ".cloudapp.net", dashAccountKey));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"));
        return bpConfigs;
    }

    @Override
    public String getDefaultFsValue(DashFileSystemConfiguration fsConfig) {
        return "wasb://" + fsConfig.getProperty(STORAGE_CONTAINER) + "@" + fsConfig.getAccountName() + ".cloudapp.net";
    }

    @Override
    public FileSystemType getFileSystemType() {
        return DASH;
    }

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs() {
        List<FileSystemScriptConfig> scriptConfigs = new ArrayList<>();
        scriptConfigs.add(new FileSystemScriptConfig("scripts/dash-local.sh", POST_INSTALL, ALL_NODES));
        scriptConfigs.add(new FileSystemScriptConfig("scripts/dash-hdfs.sh", POST_INSTALL, ONE_NODE));
        return scriptConfigs;
    }

}
