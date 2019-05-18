package com.sequenceiq.cloudbreak.template.filesystem.gcs;

import static com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType.GCS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.template.filesystem.AbstractFileSystemConfigurator;

@Component
public class GcsFileSystemConfigurator extends AbstractFileSystemConfigurator<GcsFileSystemConfigurationsView> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcsFileSystemConfigurator.class);

    private String getPrivateKey(Credential credential) {
        Json attibutesFromVault = new Json(credential.getAttributes());
        Object serviceAccountPrivateKey = attibutesFromVault.getMap().get("serviceAccountPrivateKey");
        if (serviceAccountPrivateKey == null) {
            LOGGER.debug("ServiceAccountPrivateKey isn't set.");
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
