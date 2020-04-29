package com.sequenceiq.cloudbreak.service.image;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;

@Component
public class ImageProvider {

    public Image getCurrentImageFromCatalog(String currentImageId, CloudbreakImageCatalogV3 imageCatalog)
            throws CloudbreakImageNotFoundException {
        return imageCatalog.getImages().getCdhImages().stream()
                .filter(img -> currentImageId.equals(img.getUuid()))
                .findFirst()
                .orElseThrow(() -> new CloudbreakImageNotFoundException(String.format("Image not found with id: %s", currentImageId)));
    }
}
