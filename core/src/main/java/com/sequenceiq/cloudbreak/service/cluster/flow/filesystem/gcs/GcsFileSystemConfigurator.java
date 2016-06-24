package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.gcs;

import static com.sequenceiq.cloudbreak.api.model.ExecutionType.ALL_NODES;
import static com.sequenceiq.cloudbreak.api.model.FileSystemType.GCS;
import static com.sequenceiq.cloudbreak.service.cluster.flow.ClusterLifecycleEvent.POST_INSTALL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.GcsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemScriptConfig;

@Component
public class GcsFileSystemConfigurator extends AbstractFileSystemConfigurator<GcsFileSystemConfiguration> {

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(GcsFileSystemConfiguration fsConfig) {
        String privateKey = getPrivateKey(fsConfig);
        Map<String, String> properties = Collections.singletonMap("P12KEY", privateKey);
        List<FileSystemScriptConfig> fsScriptConfigs = new ArrayList<>();
        fsScriptConfigs.add(new FileSystemScriptConfig("scripts/gcs-connector.sh", POST_INSTALL, ALL_NODES));
        fsScriptConfigs.add(new FileSystemScriptConfig("scripts/gcs-p12.sh", POST_INSTALL, ALL_NODES, properties));
        return fsScriptConfigs;
    }

    private String getPrivateKey(GcsFileSystemConfiguration fsConfig) {
        return fsConfig.getPrivateKeyEncoded();
    }

    @Override
    public List<BlueprintConfigurationEntry> getFsProperties(GcsFileSystemConfiguration fsConfig, Map<String, String> resourceProperties) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.gs.working.dir", "/"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.gs.system.bucket", fsConfig.getDefaultBucketName()));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.gs.auth.service.account.enable", "true"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.gs.auth.service.account.keyfile", "/usr/lib/hadoop/lib/gcp.p12"));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.gs.auth.service.account.email", fsConfig.getServiceAccountEmail()));
        bpConfigs.add(new BlueprintConfigurationEntry("core-site", "fs.gs.project.id", fsConfig.getProjectId()));
        return bpConfigs;
    }

    @Override
    public String getDefaultFsValue(GcsFileSystemConfiguration fsConfig) {
        return String.format("gs://%s/", fsConfig.getDefaultBucketName());
    }

    @Override
    public FileSystemType getFileSystemType() {
        return GCS;
    }
}
