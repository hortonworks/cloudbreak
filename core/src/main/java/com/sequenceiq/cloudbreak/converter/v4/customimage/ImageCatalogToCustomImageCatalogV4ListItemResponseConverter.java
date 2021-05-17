package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ListItemResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.springframework.stereotype.Component;

@Component
public class ImageCatalogToCustomImageCatalogV4ListItemResponseConverter
        extends AbstractConversionServiceAwareConverter<ImageCatalog, CustomImageCatalogV4ListItemResponse>  {

    @Override
    public CustomImageCatalogV4ListItemResponse convert(ImageCatalog source) {
        CustomImageCatalogV4ListItemResponse result = new CustomImageCatalogV4ListItemResponse();
        result.setName(source.getName());
        result.setDescription(source.getDescription());

        return result;
    }
}
