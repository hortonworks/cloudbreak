package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class UpdateImageCatalogRequestToImageCatalogConverter extends AbstractConversionServiceAwareConverter<UpdateImageCatalogV4Request, ImageCatalog> {

    @Override
    public ImageCatalog convert(UpdateImageCatalogV4Request source) {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(source.getUrl());
        imageCatalog.setName(source.getName());
        imageCatalog.setId(source.getId());
        return imageCatalog;
    }
}
