package com.sequenceiq.cloudbreak.service.image;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

@Service
public class ImageCatalogProvider {

    @Inject
    private CachedImageCatalogProvider cachedImageCatalogProvider;

    public CloudbreakImageCatalogV3 getImageCatalogV3(String catalogUrl) throws CloudbreakImageCatalogException {
        return getImageCatalogV3(catalogUrl, false);
    }

    public CloudbreakImageCatalogV3 getImageCatalogV3(String catalogUrl, boolean forceRefresh) throws CloudbreakImageCatalogException {
        if (forceRefresh) {
            cachedImageCatalogProvider.evictImageCatalogCache(catalogUrl);
        }
        return cachedImageCatalogProvider.getImageCatalogV3(catalogUrl);
    }
}
