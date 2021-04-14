package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CloudbreakVersionListProvider {

    public List<CloudbreakVersion> getVersions(CloudbreakImageCatalogV3 catalog) {
        if (catalog == null || catalog.getVersions() == null) {
            return Collections.emptyList();
        }
        Versions versions = catalog.getVersions();
        return !versions.getCloudbreakVersions().isEmpty() ?
                versions.getCloudbreakVersions() :
                versions.getFreeipaVersions();
    }
}
