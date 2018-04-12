package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.springframework.stereotype.Component;

@Component
public class ImageCatalogRequestToImageCatalogConverter extends AbstractConversionServiceAwareConverter<ImageCatalogRequest, ImageCatalog> {

    @Override
    public ImageCatalog convert(ImageCatalogRequest source) {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(source.getUrl());
        imageCatalog.setImageCatalogName(source.getName());
        return imageCatalog;
    }
}
