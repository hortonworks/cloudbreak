package com.sequenceiq.cloudbreak.service.image;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogMetaData;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogWrapper;

@Service
public class ImageCatalogProvider {

    @Inject
    private CachedImageCatalogWrapperProvider cachedImageCatalogWrapperProvider;

    public CloudbreakImageCatalogV3 getImageCatalogV3(String catalogUrl) throws CloudbreakImageCatalogException {
        return getImageCatalogV3(catalogUrl, false);
    }

    public CloudbreakImageCatalogV3 getImageCatalogV3(String catalogUrl, boolean forceRefresh) throws CloudbreakImageCatalogException {
        if (forceRefresh) {
            cachedImageCatalogWrapperProvider.evictImageCatalogCache(catalogUrl);
        }
        ImageCatalogWrapper imageCatalogWrapper = cachedImageCatalogWrapperProvider.getImageCatalogWrapper(catalogUrl);
        return imageCatalogWrapper.getImageCatalog();
    }

    public ImageCatalogMetaData getImageCatalogMetaData(String catalogUrl) throws CloudbreakImageCatalogException {
        ImageCatalogWrapper imageCatalogWrapper = cachedImageCatalogWrapperProvider.getImageCatalogWrapper(catalogUrl);
        return imageCatalogWrapper.getImageCatalogMetaData();
    }
}
