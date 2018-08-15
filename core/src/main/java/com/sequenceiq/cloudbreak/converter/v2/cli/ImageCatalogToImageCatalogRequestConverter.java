package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class ImageCatalogToImageCatalogRequestConverter
        extends AbstractConversionServiceAwareConverter<ImageCatalog, ImageCatalogRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogToImageCatalogRequestConverter.class);

    @Override
    public ImageCatalogRequest convert(ImageCatalog source) {
        ImageCatalogRequest imageCatalogRequest = new ImageCatalogRequest();
        imageCatalogRequest.setName(source.getName());
        imageCatalogRequest.setUrl(source.getImageCatalogUrl());
        return imageCatalogRequest;
    }
}
