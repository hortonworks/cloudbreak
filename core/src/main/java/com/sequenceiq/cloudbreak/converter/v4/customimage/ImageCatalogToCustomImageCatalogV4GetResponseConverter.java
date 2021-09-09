package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ImageListItemResponse;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.converter.ConversionException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import javax.inject.Inject;

@Component
public class ImageCatalogToCustomImageCatalogV4GetResponseConverter {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImageVersionsConverter imageVersionsConverter;

    public CustomImageCatalogV4GetResponse convert(ImageCatalog source) {
        CustomImageCatalogV4GetResponse result = new CustomImageCatalogV4GetResponse();
        result.setName(source.getName());
        result.setDescription(source.getDescription());
        result.setImages(source.getCustomImages().stream()
                .map(this::getImage)
                .collect(Collectors.toSet()));
        return result;
    }

    private CustomImageCatalogV4ImageListItemResponse getImage(CustomImage source) {
        CustomImageCatalogV4ImageListItemResponse result = new CustomImageCatalogV4ImageListItemResponse();
        result.setImageId(source.getName());
        result.setImageType(source.getImageType() != null ? source.getImageType().name() : null);
        result.setSourceImageId(source.getCustomizedImageId());
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
