package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.domain.FileSystemType.DASH;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.FileSystemType;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class DashFileSystemConfigurator implements FileSystemConfigurator {

    @Override
    public List<BlueprintConfigurationEntry> getBlueprintProperties(Map<String, String> fsProperties) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        String dashAccountName = fsProperties.get("account.name");
        String dashAccountKey = fsProperties.get("account.key");
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.account.key." + dashAccountName + ".cloudapp.net", dashAccountKey));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.read.factor", "1.0"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.azure.selfthrottling.write.factor", "1.0"));
        return bpConfigs;
    }

    @Override
    public String getDefaultFsValue(Map<String, String> fsProperties){
        String dashAccountName = fsProperties.get("account.name");
        return "wasb://cloudbreak@" + dashAccountName + ".cloudapp.net";
    }

    @Override
    public List<Recipe> getRecipes(Map<String, String> fsProperties) {
        return null;
    }

    @Override
    public FileSystemType getFileSystemType() {
        return DASH;
    }
}
