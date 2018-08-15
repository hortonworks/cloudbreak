package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class ImageCatalogToImageCatalogResponseConverter extends AbstractConversionServiceAwareConverter<ImageCatalog, ImageCatalogResponse> {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Override
    public ImageCatalogResponse convert(ImageCatalog source) {
        ImageCatalogResponse imageCatalogResponse = new ImageCatalogResponse();
        imageCatalogResponse.setId(source.getId());
        imageCatalogResponse.setPublicInAccount(source.isPublicInAccount());
        imageCatalogResponse.setUrl(source.getImageCatalogUrl());

        String imageCatalogName = source.getName();
        imageCatalogResponse.setUsedAsDefault(isDefault(imageCatalogName));
        imageCatalogResponse.setName(imageCatalogName);

        return imageCatalogResponse;
    }

    private boolean isDefault(String imageCatalogName) {
        String defaultImageCatalogName = imageCatalogService.getDefaultImageCatalogName();
        return imageCatalogName.equals(defaultImageCatalogName) || (defaultImageCatalogName == null && imageCatalogService.isEnvDefault(imageCatalogName));
    }
}
