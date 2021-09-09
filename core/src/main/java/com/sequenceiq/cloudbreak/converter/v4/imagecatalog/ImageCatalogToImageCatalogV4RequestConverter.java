package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class ImageCatalogToImageCatalogV4RequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogToImageCatalogV4RequestConverter.class);

    public ImageCatalogV4Request convert(ImageCatalog source) {
        ImageCatalogV4Request imageCatalogRequest = new ImageCatalogV4Request();
        imageCatalogRequest.setName(source.getName());
        imageCatalogRequest.setUrl(source.getImageCatalogUrl());
        imageCatalogRequest.setDescription(source.getDescription());
        return imageCatalogRequest;
    }
}
