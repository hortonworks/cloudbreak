package com.sequenceiq.cloudbreak.service.image;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class CurrentImageUsageCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentImageUsageCondition.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ImageProvider imageProvider;

    public boolean currentImageUsedOnInstances(Long stackId, String currentImageId) {
        Set<Image> imagesFromInstances = getImagesFromInstanceMetadata(stackId);
        LOGGER.debug("The current image of the stack: {}. The available images on instances: {}", currentImageId, getImageIds(imagesFromInstances));
        return imagesFromInstances.stream()
                .anyMatch(image -> !image.getImageId().equals(currentImageId));
    }

    private Set<Image> getImagesFromInstanceMetadata(Long stackId) {
        return instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(stackId)
                .stream()
                .map(InstanceMetaData::getImage)
                .filter(json -> !json.getMap().isEmpty())
                .map(this::convertJsonToImage)
                .collect(Collectors.toSet());
    }

    private Image convertJsonToImage(Json imageJson) {
        return imageProvider.convertJsonToImage(imageJson);
    }

    private Set<String> getImageIds(Set<Image> imagesFromInstances) {
        return imagesFromInstances.stream()
                .map(Image::getImageId)
                .collect(Collectors.toSet());
    }
}
