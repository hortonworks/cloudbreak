package com.sequenceiq.cloudbreak.service.image;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    public boolean currentImageUsedOnInstances(Long stackId, String currentImageId) {
        Map<String, String> imageByInstances = getImagesByInstance(stackId);
        LOGGER.debug("The current image of the stack: {}. The available images on instances: {}", currentImageId, imageByInstances);
        return !imageByInstances.isEmpty() && imageByInstances.values().stream().allMatch(imageIdOnInstance -> imageIdOnInstance.equals(currentImageId));
    }

    private Map<String, String> getImagesByInstance(Long stackId) {
        return instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(stackId)
                .stream()
                .filter(instanceMetaData -> !instanceMetaData.getImage().getMap().isEmpty())
                .collect(Collectors.toMap(InstanceMetaData::getInstanceId, instanceMetaData -> convertJsonToImage(instanceMetaData.getImage())));
    }

    private String convertJsonToImage(Json image) {
        return imageConverter.convertJsonToImage(image).getImageId();
    }
}
