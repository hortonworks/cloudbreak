package com.sequenceiq.cloudbreak.converter.v4.customimage;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetImageResponse;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.converter.ConversionException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class CustomImageToCustomImageCatalogV4GetImageResponseConverter {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImageVersionsConverter imageVersionsConverter;

    @Inject
    private VmImagesConverter vmImagesConverter;

    public CustomImageCatalogV4GetImageResponse convert(CustomImage source) {
        CustomImageCatalogV4GetImageResponse result = new CustomImageCatalogV4GetImageResponse();
        result.setImageId(source.getName());
        result.setImageType(source.getImageType() != null ? source.getImageType().name() : null);
        result.setSourceImageId(source.getCustomizedImageId());
        result.setBaseParcelUrl(source.getBaseParcelUrl());
        result.setVmImages(vmImagesConverter.convert(source.getVmImage()));
        result.setImageDate(source.getCreated());

        try {
            Image image = imageCatalogService.getSourceImageByImageType(source).getImage();
            result.setSourceImageDate(image.getCreated());
            result.setVersions(imageVersionsConverter.convert(image));
            result.setCloudProvider(image.getImageSetsByProvider().keySet().stream().findFirst().orElse(null));
        } catch (Exception ex) {
            throw new ConversionException(ex.getMessage());
        }

        return result;
    }
}
