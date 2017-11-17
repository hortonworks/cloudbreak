package com.sequenceiq.cloudbreak.service.image;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

@Service
public class ImageCatalogProvider {

    @Inject
    private CachedImageCatalogProvider cachedImageCatalogProvider;

    public CloudbreakImageCatalogV2 getImageCatalogV2(String catalogUrl) throws CloudbreakImageCatalogException {
        return getImageCatalogV2(catalogUrl, false);
    }

    public CloudbreakImageCatalogV2 getImageCatalogV2(String catalogUrl, boolean forceRefresh) throws CloudbreakImageCatalogException {
        if (forceRefresh) {
            cachedImageCatalogProvider.evictImageCatalogCache(catalogUrl);
        }
        return cachedImageCatalogProvider.getImageCatalogV2(catalogUrl);
    }
}
