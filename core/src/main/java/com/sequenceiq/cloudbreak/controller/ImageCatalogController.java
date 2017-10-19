package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.ImageCatalogEndpoint;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.converter.ImagesToImagesResponseJsonConverter;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class ImageCatalogController implements ImageCatalogEndpoint {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImagesToImagesResponseJsonConverter converter;

    @Override
    public ImagesResponse getImagesByProvider(String platform) throws Exception {
        Images images = imageCatalogService.getImages(platform);
        return converter.convert(images);
    }
}
