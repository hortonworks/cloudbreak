package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class ImageCatalogV4RequestToImageCatalogConverter {

    public ImageCatalog convert(ImageCatalogV4Request source) {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(source.getName());
        imageCatalog.setDescription(source.getDescription());
        imageCatalog.setImageCatalogUrl(source.getUrl());

        return imageCatalog;
    }
}
