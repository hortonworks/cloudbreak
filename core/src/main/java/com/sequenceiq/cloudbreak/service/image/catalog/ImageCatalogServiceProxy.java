package com.sequenceiq.cloudbreak.service.image.catalog;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogMetaData;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ImageCatalogServiceProxy implements ImageCatalogService {

    @Inject
    private VersionBasedImageCatalogService versionBasedImageCatalogService;

    @Inject
    private AdvertisedImageCatalogService advertisedImageCatalogService;

    @Override
    public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        return getImageCatalogService(imageCatalogV3).getImages(imageCatalogV3, imageFilter);
    }

    @Override
    public void validate(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        getImageCatalogService(imageCatalogV3).validate(imageCatalogV3);
    }

    @Override
    public ImageFilterResult getImageFilterResult(CloudbreakImageCatalogV3 imageCatalogV3) {
        return getImageCatalogService(imageCatalogV3).getImageFilterResult(imageCatalogV3);
    }

    @Override
    public ImageCatalogMetaData getImageCatalogMetaData(CloudbreakImageCatalogV3 imageCatalogV3) {
        return getImageCatalogService(imageCatalogV3).getImageCatalogMetaData(imageCatalogV3);
    }

    private ImageCatalogService getImageCatalogService(CloudbreakImageCatalogV3 imageCatalogV3) {
        return imageCatalogV3.getVersions() == null ? advertisedImageCatalogService : versionBasedImageCatalogService;
    }
}
