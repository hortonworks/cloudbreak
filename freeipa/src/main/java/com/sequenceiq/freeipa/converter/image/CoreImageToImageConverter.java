package com.sequenceiq.freeipa.converter.image;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

@Component
public class CoreImageToImageConverter {

    public Image convert(com.sequenceiq.cloudbreak.cloud.model.catalog.Image source) {
        return new Image(source.getCreated(),
                source.getDate(),
                source.getDescription(),
                source.getOs(),
                source.getUuid(),
                source.getImageSetsByProvider(),
                source.getOsType(),
                source.getPackageVersions(),
                source.isAdvertised(),
                source.getArchitecture());
    }
}
