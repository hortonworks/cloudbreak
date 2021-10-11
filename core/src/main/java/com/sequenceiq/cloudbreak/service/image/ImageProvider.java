package com.sequenceiq.cloudbreak.service.image;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class ImageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageProvider.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public com.sequenceiq.cloudbreak.cloud.model.catalog.Image getCurrentImageFromCatalog(String currentImageId, CloudbreakImageCatalogV3 imageCatalog)
            throws CloudbreakImageNotFoundException {
        return imageCatalog.getImages().getCdhImages().stream()
                .filter(img -> currentImageId.equals(img.getUuid()))
                .findFirst()
                .orElseThrow(() -> new CloudbreakImageNotFoundException(String.format("Image not found with id: %s", currentImageId)));
    }

    public boolean filterCurrentImage(Long stackId, String currentImageId) {
        Set<Image> imagesFromInstances = getImagesFromInstanceMetadata(stackId);
        LOGGER.debug("The current image of the stack: {}. The available images on instances: {}", currentImageId, getImageIds(imagesFromInstances));
        return imagesFromInstances.stream()
                .anyMatch(image -> !image.getImageId().equals(currentImageId));
    }

    private Set<Image> getImagesFromInstanceMetadata(Long stackId) {
        return instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stackId)
                .stream()
                .map(InstanceMetaData::getImage)
                .filter(json -> !json.getMap().isEmpty())
                .map(this::convertJsonToImage)
                .collect(Collectors.toSet());
    }

    public Image convertJsonToImage(Json imageJson) {
        try {
            return imageJson.get(Image.class);
        } catch (IOException e) {
            String message = "Failed to convert Json to Image";
            LOGGER.error(message, e);
            throw new CloudbreakRuntimeException(message, e);
        }
    }

    private Set<String> getImageIds(Set<Image> imagesFromInstances) {
        return imagesFromInstances.stream()
                .map(Image::getImageId)
                .collect(Collectors.toSet());
    }
}
