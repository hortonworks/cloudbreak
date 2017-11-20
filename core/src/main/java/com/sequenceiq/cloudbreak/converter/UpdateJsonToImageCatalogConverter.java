package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.UpdateImageCatalogRequest;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class UpdateJsonToImageCatalogConverter extends AbstractConversionServiceAwareConverter<UpdateImageCatalogRequest, ImageCatalog> {

    @Override
    public ImageCatalog convert(UpdateImageCatalogRequest source) {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(source.getUrl());
        imageCatalog.setImageCatalogName(source.getName());
        imageCatalog.setId(source.getId());
        return imageCatalog;
    }
}
