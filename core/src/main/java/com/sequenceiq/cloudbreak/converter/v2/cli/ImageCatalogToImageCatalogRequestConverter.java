package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class ImageCatalogToImageCatalogRequestConverter
        extends AbstractConversionServiceAwareConverter<ImageCatalog, ImageCatalogV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogToImageCatalogRequestConverter.class);

    @Override
    public ImageCatalogV4Request convert(ImageCatalog source) {
        ImageCatalogV4Request imageCatalogRequest = new ImageCatalogV4Request();
        imageCatalogRequest.setName(source.getName());
        imageCatalogRequest.setUrl(source.getImageCatalogUrl());
        return imageCatalogRequest;
    }
}
