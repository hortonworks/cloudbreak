package com.sequenceiq.cloudbreak.template.filesystem.gcs;

import static com.sequenceiq.cloudbreak.api.model.ExecutionType.ALL_NODES;
import static com.sequenceiq.cloudbreak.api.model.ExecutionType.ONE_NODE;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType.GCS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.template.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemScriptConfig;

@Component
public class GcsFileSystemConfigurator extends AbstractFileSystemConfigurator<GcsFileSystemConfigurationsView> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcsFileSystemConfigurator.class);

    @Inject
    private VaultService vaultService;

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential) {
        Map<String, String> properties = Collections.singletonMap("P12KEY", getPrivateKey(credential));
        List<FileSystemScriptConfig> fsScriptConfigs = new ArrayList<>();
        fsScriptConfigs.add(new FileSystemScriptConfig("scripts/gcs-p12.sh", POST_AMBARI_START, ALL_NODES, properties));
        fsScriptConfigs.add(new FileSystemScriptConfig("scripts/gcs-connector-local.sh", POST_AMBARI_START, ALL_NODES));
        fsScriptConfigs.add(new FileSystemScriptConfig("scripts/gcs-connector-hdfs.sh", POST_CLUSTER_INSTALL, ONE_NODE));
        return fsScriptConfigs;
    }

    private String getPrivateKey(Credential credential) {
        Json attibutesFromVault = new Json(vaultService.resolveSingleValue(credential.getAttributes()));
        Object serviceAccountPrivateKey = attibutesFromVault.getMap().get("serviceAccountPrivateKey");
        if (serviceAccountPrivateKey == null) {
            LOGGER.warn("ServiceAccountPrivateKey isn't set.");
            return "";
        }
        return serviceAccountPrivateKey.toString();
    }

    @Override
    public FileSystemType getFileSystemType() {
        return GCS;
    }

    @Override
    public String getProtocol() {
        return GCS.getProtocol();
    }
}
