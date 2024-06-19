package com.sequenceiq.freeipa.converter.image;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

@Component
public class ImageToCoreImageConverter {

    public com.sequenceiq.cloudbreak.cloud.model.catalog.Image convert(Image source) {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image coreImage = new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(source.getDate(),
                source.getCreated(),
                null,
                source.getDescription(),
                source.getOs(),
                source.getUuid(),
                null,
                null,
                source.getImageSetsByProvider(),
                null,
                source.getOsType(),
                source.getPackageVersions(),
                null,
                null,
                null,
                source.isAdvertised(),
                null,
                null,
                null);
        return coreImage;
    }
}
