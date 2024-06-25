package com.sequenceiq.freeipa.converter.image;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

@Component
public class ImageToCoreImageConverter {

    public com.sequenceiq.cloudbreak.cloud.model.catalog.Image convert(Image source) {
        return com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withDate(source.getDate())
                .withCreated(source.getCreated())
                .withDescription(source.getDescription())
                .withOs(source.getOs())
                .withUuid(source.getUuid())
                .withImageSetsByProvider(source.getImageSetsByProvider())
                .withOsType(source.getOsType())
                .withPackageVersions(source.getPackageVersions())
                .withAdvertised(source.isAdvertised())
                .build();
    }
}
