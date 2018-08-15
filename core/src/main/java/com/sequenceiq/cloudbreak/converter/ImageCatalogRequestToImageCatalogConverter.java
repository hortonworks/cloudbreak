package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class ImageCatalogRequestToImageCatalogConverter extends AbstractConversionServiceAwareConverter<ImageCatalogRequest, ImageCatalog> {

    @Override
    public ImageCatalog convert(ImageCatalogRequest source) {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(source.getUrl());
        imageCatalog.setName(source.getName());
        return imageCatalog;
    }
}
