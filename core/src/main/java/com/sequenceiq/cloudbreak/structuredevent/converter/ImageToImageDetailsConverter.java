package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;

@Component
public class ImageToImageDetailsConverter {

    public ImageDetails convert(Image source) {
        ImageDetails imageDetails = new ImageDetails();
        imageDetails.setImageName(source.getImageName());
        imageDetails.setOs(source.getOs());
        imageDetails.setOsType(source.getOsType());
        imageDetails.setImageCatalogUrl(source.getImageCatalogUrl());
        imageDetails.setImageId(source.getImageId());
        imageDetails.setImageCatalogName(source.getImageCatalogName());
        imageDetails.setPackageVersions(source.getPackageVersions());
        imageDetails.setImageArchitecture(source.getArchitecture());
        return imageDetails;
    }
}
