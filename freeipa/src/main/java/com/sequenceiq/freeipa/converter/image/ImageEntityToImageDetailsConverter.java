package com.sequenceiq.freeipa.converter.image;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Component
public class ImageEntityToImageDetailsConverter {

    public ImageDetails convert(ImageEntity source) {
        ImageDetails imageDetails = new ImageDetails();
        imageDetails.setImageName(source.getImageName());
        imageDetails.setOs(source.getOs());
        imageDetails.setOsType(source.getOsType());
        imageDetails.setImageCatalogUrl(source.getImageCatalogUrl());
        imageDetails.setImageId(source.getImageId());
        imageDetails.setImageCatalogName(source.getImageCatalogName());
        imageDetails.setImageArchitecture(source.getArchitecture());
        return imageDetails;
    }
}
