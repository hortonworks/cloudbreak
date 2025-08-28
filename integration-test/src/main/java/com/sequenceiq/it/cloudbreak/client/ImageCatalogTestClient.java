package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateWithoutNameLoggingAction;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogGetImagesByNameAction;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogGetImagesFromDefaultCatalogAction;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogSetAsDefaultAction;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

@Service
public class ImageCatalogTestClient {

    public Action<ImageCatalogTestDto, CloudbreakClient> createV4() {
        return new ImageCatalogCreateAction();
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> createIfNotExistV4() {
        return new ImageCatalogCreateIfNotExistsAction();
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> createWithoutNameV4() {
        return new ImageCatalogCreateWithoutNameLoggingAction();
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> deleteV4() {
        return new ImageCatalogDeleteAction();
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> getV4() {
        return new ImageCatalogGetAction();
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> getV4WithAdvertisedImages() {
        return new ImageCatalogGetAction(true);
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> getV4WithAllImages() {
        return new ImageCatalogGetAction(true, false);
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> getImagesByNameV4() {
        return new ImageCatalogGetImagesByNameAction();
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> getImagesByNameV4(CloudPlatform cloudPlatform, boolean defaultOnly) {
        return new ImageCatalogGetImagesByNameAction(cloudPlatform, defaultOnly);
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> getImagesByNameV4(String stackName) {
        return new ImageCatalogGetImagesByNameAction(stackName);
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> getImagesFromDefaultCatalog() {
        return new ImageCatalogGetImagesFromDefaultCatalogAction();
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> getImagesFromDefaultCatalog(CloudPlatform platform) {
        return new ImageCatalogGetImagesFromDefaultCatalogAction(platform);
    }

    public Action<ImageCatalogTestDto, CloudbreakClient> setAsDefault() {
        return new ImageCatalogSetAsDefaultAction();
    }

}
