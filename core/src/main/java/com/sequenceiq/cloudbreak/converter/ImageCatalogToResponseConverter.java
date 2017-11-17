package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class ImageCatalogToResponseConverter extends AbstractConversionServiceAwareConverter<ImageCatalog, ImageCatalogResponse> {

    @Override
    public ImageCatalogResponse convert(ImageCatalog source) {
        ImageCatalogResponse imageCatalogResponse = new ImageCatalogResponse();
        imageCatalogResponse.setId(source.getId());
        imageCatalogResponse.setPublicInAccount(source.isPublicInAccount());
        imageCatalogResponse.setName(source.getImageCatalogName());
        imageCatalogResponse.setUrl(source.getImageCatalogUrl());
        return imageCatalogResponse;
    }
}
