package com.sequenceiq.cloudbreak.service.image;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.common.model.OsType;

@Component
public class CurrentImageUsageCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentImageUsageCondition.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ImageConverter imageConverter;

    public boolean isCurrentImageUsedOnInstances(Long stackId, String currentImageName, String currentImageId) {
        Objects.requireNonNull(currentImageId, "currentImageId");
        Map<String, Image> imageByInstances = getImagesByInstance(stackId);
        LOGGER.debug("Checking current image usage on instances. imageName: {}, imageId: {}, instances: {}",
                currentImageName, currentImageId, imageByInstances);
        return !imageByInstances.isEmpty() && imageByInstances.values().stream()
                .allMatch(instanceImage -> (currentImageName != null && currentImageName.equals(instanceImage.getImageName()))
                        || currentImageId.equals(instanceImage.getImageId()));
    }

    public Set<OsType> getOSUsedByInstances(Long stackId) {
        return getImagesByInstance(stackId)
                .values()
                .stream()
                .map(image -> OsType.getByOsTypeStringWithCentos7Fallback(image.getOsType()))
                .collect(Collectors.toSet());
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
