package com.sequenceiq.cloudbreak.converter.v4.customimage;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4VmImageResponse;
import com.sequenceiq.cloudbreak.domain.VmImage;

@Component
public class VmImagesConverter {

    public Set<CustomImageCatalogV4VmImageResponse> convert(Set<VmImage> vmImages) {
        return vmImages.stream()
                .map(this::toResponse)
                .collect(Collectors.toSet());
    }

    private CustomImageCatalogV4VmImageResponse toResponse(VmImage vmImage) {
        CustomImageCatalogV4VmImageResponse result = new CustomImageCatalogV4VmImageResponse();
        result.setRegion(vmImage.getRegion());
        result.setImageReference(vmImage.getImageReference());
        return result;
    }
}
