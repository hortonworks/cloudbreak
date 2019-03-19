package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog.ImageCatalogCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog.ImageCatalogCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog.ImageCatalogCreateWithoutNameLoggingAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog.ImageCatalogDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog.ImageCatalogGetAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog.ImageCatalogGetImagesByNameAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog.ImageCatalogGetImagesFromDefaultCatalogAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog.ImageCatalogSetAsDefaultAction;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;

@Service
public class ImageCatalogTestClient {

    public Action<ImageCatalogTestDto> createV4() {
        return new ImageCatalogCreateAction();
    }

    public Action<ImageCatalogTestDto> createIfNotExistV4() {
        return new ImageCatalogCreateIfNotExistsAction();
    }

    public Action<ImageCatalogTestDto> createWithoutNameV4() {
        return new ImageCatalogCreateWithoutNameLoggingAction();
    }

    public Action<ImageCatalogTestDto> deleteV4() {
        return new ImageCatalogDeleteAction();
    }

    public Action<ImageCatalogTestDto> getV4() {
        return new ImageCatalogGetAction();
    }

    public Action<ImageCatalogTestDto> getV4(Boolean withImages) {
        return new ImageCatalogGetAction(withImages);
    }

    public Action<ImageCatalogTestDto> getImagesByNameV4() {
        return new ImageCatalogGetImagesByNameAction();
    }

    public Action<ImageCatalogTestDto> getImagesByNameV4(String stackName) {
        return new ImageCatalogGetImagesByNameAction(stackName);
    }

    public Action<ImageCatalogTestDto> getImagesFromDefaultCatalog() {
        return new ImageCatalogGetImagesFromDefaultCatalogAction();
    }

    public Action<ImageCatalogTestDto> getImagesFromDefaultCatalog(CloudPlatform platform) {
        return new ImageCatalogGetImagesFromDefaultCatalogAction(platform);
    }

    public Action<ImageCatalogTestDto> setAsDefault() {
        return new ImageCatalogSetAsDefaultAction();
    }

}
