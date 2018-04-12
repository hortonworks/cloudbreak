package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.UpdateImageCatalogRequest;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.springframework.stereotype.Component;

@Component
public class UpdateImageCatalogRequestToImageCatalogConverter extends AbstractConversionServiceAwareConverter<UpdateImageCatalogRequest, ImageCatalog> {

    @Override
    public ImageCatalog convert(UpdateImageCatalogRequest source) {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(source.getUrl());
        imageCatalog.setImageCatalogName(source.getName());
        imageCatalog.setId(source.getId());
        return imageCatalog;
    }
}
