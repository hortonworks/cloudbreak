package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4CreateImageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4VmImageRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.VmImage;
import com.sequenceiq.common.api.type.ImageType;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CustomImageCatalogV4CreateImageRequestToCustomImageConverter
        extends AbstractConversionServiceAwareConverter<CustomImageCatalogV4CreateImageRequest, CustomImage> {

    @Override
    public CustomImage convert(CustomImageCatalogV4CreateImageRequest source) {
        CustomImage result = new CustomImage();
        result.setImageType(convertImageType(source.getImageType()));
        result.setBaseParcelUrl(source.getBaseParcelUrl());
        result.setCustomizedImageId(source.getSourceImageId());
        result.setVmImage(getVmImages(source.getVmImages()));

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
