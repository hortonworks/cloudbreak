package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4UpdateImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4VmImageResponse;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.VmImage;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CustomImageToCustomImageCatalogV4UpdateImageResponseConverter {

    public CustomImageCatalogV4UpdateImageResponse convert(CustomImage source) {
        CustomImageCatalogV4UpdateImageResponse result = new CustomImageCatalogV4UpdateImageResponse();
        result.setImageId(source.getName());
        result.setImageType(source.getImageType() != null ? source.getImageType().name() : null);
        result.setSourceImageId(source.getCustomizedImageId());
        result.setBaseParcelUrl(source.getBaseParcelUrl());
        result.setVmImages(getVmImages(source.getVmImage()));

        return result;
    }

    private Set<CustomImageCatalogV4VmImageResponse> getVmImages(Set<VmImage> vmImages) {
        return vmImages.stream().map(vm -> {
            CustomImageCatalogV4VmImageResponse result = new CustomImageCatalogV4VmImageResponse();
            result.setRegion(vm.getRegion());
            result.setImageReference(vm.getImageReference());

            return result;
        }).collect(Collectors.toSet());
    }
}
