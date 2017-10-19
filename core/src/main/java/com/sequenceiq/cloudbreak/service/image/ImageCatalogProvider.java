package com.sequenceiq.cloudbreak.service.image;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

@Service
public class ImageCatalogProvider {
    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Inject
    private CachedImageCatalogProvider cachedImageCatalogProvider;

    public CloudbreakImageCatalogV2 getImageCatalogV2() throws CloudbreakImageCatalogException {
        return getImageCatalogV2(false);
    }

    public CloudbreakImageCatalogV2 getImageCatalogV2(boolean forceRefresh) throws CloudbreakImageCatalogException {
        if (forceRefresh) {
            cachedImageCatalogProvider.evictImageCatalogCache(defaultCatalogUrl);
        }
        return cachedImageCatalogProvider.getImageCatalogV2(defaultCatalogUrl);
    }
}
