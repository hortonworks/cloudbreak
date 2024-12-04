package com.sequenceiq.cloudbreak.service.image;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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
    private ImageConverter imageConverter;

    public boolean isCurrentImageUsedOnInstances(Long stackId, String currentImageId) {
        Map<String, Image> imageByInstances = getImagesByInstance(stackId);
        LOGGER.debug("The current image of the stack: {}. The available images on instances: {}", currentImageId, imageByInstances);
        return !imageByInstances.isEmpty() && imageByInstances.values().stream()
                .map(Image::getImageId)
                .allMatch(imageIdOnInstance -> imageIdOnInstance.equals(currentImageId));
    }

    public boolean isCurrentOsUsedOnInstances(Long stackId, String currentImageOs) {
        Map<String, Image> imageByInstances = getImagesByInstance(stackId);
        return !imageByInstances.isEmpty() && imageByInstances.values().stream()
                .map(Image::getOs)
                .allMatch(osUsedOnInstance -> StringUtils.equalsIgnoreCase(osUsedOnInstance, currentImageOs));
    }

    private Map<String, Image> getImagesByInstance(Long stackId) {
        return instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(stackId)
                .stream()
                .filter(instanceMetaData -> !instanceMetaData.getImage().getMap().isEmpty())
                .collect(Collectors.toMap(InstanceMetaData::getInstanceId, instanceMetaData -> convertJsonToImage(instanceMetaData.getImage())));
    }

    private Image convertJsonToImage(Json image) {
        return imageConverter.convertJsonToImage(image);
    }
}
