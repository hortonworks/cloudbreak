package com.sequenceiq.cloudbreak.converter.v4.customimage;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateImageResponse;
import com.sequenceiq.cloudbreak.domain.CustomImage;

@Component
public class CustomImageToCustomImageCatalogV4CreateImageResponseConverter {

    @Inject
    private VmImagesConverter vmImagesConverter;

    public CustomImageCatalogV4CreateImageResponse convert(CustomImage source) {
        CustomImageCatalogV4CreateImageResponse result = new CustomImageCatalogV4CreateImageResponse();
        result.setImageId(source.getName());
        result.setImageType(source.getImageType() != null ? source.getImageType().name() : null);
        result.setSourceImageId(source.getCustomizedImageId());
        result.setBaseParcelUrl(source.getBaseParcelUrl());
        result.setVmImages(vmImagesConverter.convert(source.getVmImage()));

        return result;
    }
}
