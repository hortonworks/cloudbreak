package com.sequenceiq.cloudbreak.service.image.catalog;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

interface ImageCatalogService {
    StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter);

    void validate(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException;

    ImageFilterResult getImageFilterResult(CloudbreakImageCatalogV3 imageCatalogV3);
}
