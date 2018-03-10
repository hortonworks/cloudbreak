package com.sequenceiq.cloudbreak.blueprint.filesystem.gcs;

import static com.sequenceiq.cloudbreak.api.model.ExecutionType.ALL_NODES;
import static com.sequenceiq.cloudbreak.api.model.ExecutionType.ONE_NODE;
import static com.sequenceiq.cloudbreak.api.model.FileSystemType.GCS;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.POST_CLUSTER_INSTALL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.GcsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemScriptConfig;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class GcsFileSystemConfigurator extends AbstractFileSystemConfigurator<GcsFileSystemConfiguration> {

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential, GcsFileSystemConfiguration fsConfig) {
        String privateKey = getPrivateKey(credential);
        Map<String, String> properties = Collections.singletonMap("P12KEY", privateKey);
        List<FileSystemScriptConfig> fsScriptConfigs = new ArrayList<>();
        fsScriptConfigs.add(new FileSystemScriptConfig("scripts/gcs-p12.sh", POST_AMBARI_START, ALL_NODES, properties));
        fsScriptConfigs.add(new FileSystemScriptConfig("scripts/gcs-connector-local.sh", POST_AMBARI_START, ALL_NODES));
        fsScriptConfigs.add(new FileSystemScriptConfig("scripts/gcs-connector-hdfs.sh", POST_CLUSTER_INSTALL, ONE_NODE));
        return fsScriptConfigs;
    }

    private String getPrivateKey(Credential credential) {
        return credential.getAttributes().getMap().get("serviceAccountPrivateKey").toString();
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
