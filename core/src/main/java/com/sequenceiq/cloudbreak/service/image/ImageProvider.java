package com.sequenceiq.cloudbreak.service.image;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

@Component
public class ImageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageProvider.class);

    public Image getCurrentImageFromCatalog(String currentImageId, CloudbreakImageCatalogV3 imageCatalog)
            throws CloudbreakImageNotFoundException {
        return imageCatalog.getImages().getCdhImages().stream()
                .filter(img -> currentImageId.equals(img.getUuid()))
                .findFirst()
                .orElseThrow(() -> new CloudbreakImageNotFoundException(String.format("Image not found with id: %s", currentImageId)));
    }

    public com.sequenceiq.cloudbreak.cloud.model.Image convertJsonToImage(Json imageJson) {
        try {
            return imageJson.get(com.sequenceiq.cloudbreak.cloud.model.Image.class);
        } catch (IOException e) {
            String message = "Failed to convert Json to Image";
            LOGGER.error(message, e);
            throw new CloudbreakRuntimeException(message, e);
        }
    }
}
