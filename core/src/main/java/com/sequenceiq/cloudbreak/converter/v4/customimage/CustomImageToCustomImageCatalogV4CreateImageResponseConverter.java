package com.sequenceiq.cloudbreak.converter.v4.customimage;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4VmImageResponse;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.VmImage;

@Component
public class CustomImageToCustomImageCatalogV4CreateImageResponseConverter {

    public CustomImageCatalogV4CreateImageResponse convert(CustomImage source) {
        CustomImageCatalogV4CreateImageResponse result = new CustomImageCatalogV4CreateImageResponse();
        result.setImageId(source.getName());
        result.setImageType(source.getImageType() != null ? source.getImageType().name() : null);
        result.setSourceImageId(source.getCustomizedImageId());
        result.setBaseParcelUrl(source.getBaseParcelUrl());
        result.setVmImages(getVmImages(source.getVmImage()));

        return result;
    }

    private Set<CustomImageCatalogV4VmImageResponse> getVmImages(Set<VmImage> vmImages) {
        return vmImages.stream().map(vmImage -> {
            CustomImageCatalogV4VmImageResponse vmImageResponse = new CustomImageCatalogV4VmImageResponse();
            vmImageResponse.setImageReference(vmImage.getImageReference());
            vmImageResponse.setRegion(vmImage.getRegion());

            return vmImageResponse;
        }).collect(Collectors.toSet());
    }
}
