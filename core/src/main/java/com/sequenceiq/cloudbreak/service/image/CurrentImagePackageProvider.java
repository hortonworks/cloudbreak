package com.sequenceiq.cloudbreak.service.image;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class CurrentImagePackageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentImagePackageProvider.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ImageConverter imageConverter;

    public boolean currentInstancesContainsPackage(Long stackId, List<Image> cdhImagesFromCatalog, ImagePackageVersion packageVersion) {
        Map<String, com.sequenceiq.cloudbreak.cloud.model.Image> imagesByInstance = getImagesByInstance(stackId);
        LOGGER.debug("The available packages on instances: {}", imagesByInstance);
        return imagesByInstance.entrySet().stream()
                .allMatch(entry ->
                        modelImageContainsPackage(entry.getValue(), packageVersion) ||
                        catalogImageContainsPackage(cdhImagesFromCatalog, packageVersion, entry));
    }

    private boolean catalogImageContainsPackage(List<Image> cdhImagesFromCatalog, ImagePackageVersion packageVersion,
            Map.Entry<String, com.sequenceiq.cloudbreak.cloud.model.Image> entry) {
        return cdhImagesFromCatalog.stream().anyMatch(imageFromCatalog -> currentImageContainsPackage(packageVersion, entry, imageFromCatalog));
    }

    private boolean modelImageContainsPackage(com.sequenceiq.cloudbreak.cloud.model.Image image, ImagePackageVersion packageVersion) {
        return image.getPackageVersions() != null && image.getPackageVersions().containsKey(packageVersion.getKey());
    }

    private Map<String, com.sequenceiq.cloudbreak.cloud.model.Image> getImagesByInstance(Long stackId) {
        return instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(stackId)
                .stream()
                .filter(instanceMetaData -> !instanceMetaData.getImage().getMap().isEmpty())
                .collect(Collectors.toMap(InstanceMetaData::getInstanceId,
                        instanceMetaData -> convertJsonToImage(instanceMetaData.getImage())));
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image convertJsonToImage(Json image) {
        return imageConverter.convertJsonToImage(image);
    }

    private boolean currentImageContainsPackage(ImagePackageVersion packageVersion, Map.Entry<String, com.sequenceiq.cloudbreak.cloud.model.Image> entry,
            Image imageFromCatalog) {
        return imageFromCatalog.getUuid().equals(entry.getValue().getImageId()) && imageFromCatalog.getPackageVersions().containsKey(packageVersion.getKey());
    }
}
