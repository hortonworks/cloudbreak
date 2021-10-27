package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4UpdateImageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4VmImageRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.VmImage;
import com.sequenceiq.common.api.type.ImageType;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CustomImageCatalogV4UpdateImageRequestToCustomImageConverter
        extends AbstractConversionServiceAwareConverter<CustomImageCatalogV4UpdateImageRequest, CustomImage> {

    @Override
    public CustomImage convert(CustomImageCatalogV4UpdateImageRequest source) {
        CustomImage result = new CustomImage();
        result.setImageType(source.getImageType() != null ? convertImageType(source.getImageType()) : null);
        result.setBaseParcelUrl(source.getBaseParcelUrl());
        result.setCustomizedImageId(source.getSourceImageId());
        result.setVmImage(source.getVmImages() != null ? getVmImages(source.getVmImages()) : null);

        return result;
    }

    private Set<VmImage> getVmImages(Set<CustomImageCatalogV4VmImageRequest> vmImages) {
        return vmImages.stream().map(vm -> {
            VmImage vmImage = new VmImage();
            vmImage.setImageReference(vm.getImageReference());
            vmImage.setRegion(vm.getRegion());

            return vmImage;
        }).collect(Collectors.toSet());
    }

    private ImageType convertImageType(String imageType) {
        ImageType type = ImageType.valueOf(imageType);
        return type == ImageType.DATAHUB || type == ImageType.DATALAKE ? ImageType.RUNTIME : type;
    }
}
