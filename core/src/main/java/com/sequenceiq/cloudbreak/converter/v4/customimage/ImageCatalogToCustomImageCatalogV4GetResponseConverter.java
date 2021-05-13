package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ImageCatalogToCustomImageCatalogV4GetResponseConverter
        extends AbstractConversionServiceAwareConverter<ImageCatalog, CustomImageCatalogV4GetResponse> {

    @Override
    public CustomImageCatalogV4GetResponse convert(ImageCatalog source) {
        CustomImageCatalogV4GetResponse result = new CustomImageCatalogV4GetResponse();
        result.setName(source.getName());
        result.setDescription(source.getDescription());
        result.setImageIds(source.getCustomImages().stream().map(ci -> ci.getName()).collect(Collectors.toSet()));

        return result;
    }
}
