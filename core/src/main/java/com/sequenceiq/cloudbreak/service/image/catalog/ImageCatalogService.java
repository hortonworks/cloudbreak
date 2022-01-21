package com.sequenceiq.cloudbreak.service.image.catalog;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogMetaData;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

interface ImageCatalogService {

    VersionComparator VERSION_COMPARATOR = new VersionComparator();

    StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter);

    void validate(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException;

    ImageFilterResult getImageFilterResult(CloudbreakImageCatalogV3 imageCatalogV3);

    default ImageCatalogMetaData getImageCatalogMetaData(CloudbreakImageCatalogV3 imageCatalogV3) {
        ImageFilterResult imageFilterResult = getImageFilterResult(imageCatalogV3);
        List<String> runtimes = imageFilterResult.getImages()
                .stream()
                .map(Image::getVersion)
                .distinct()
                .map(version -> (Versioned) () -> version)
                .sorted(VERSION_COMPARATOR.reversed())
                .map(Versioned::getVersion)
                .collect(toList());
        return new ImageCatalogMetaData(runtimes);
    }
}
