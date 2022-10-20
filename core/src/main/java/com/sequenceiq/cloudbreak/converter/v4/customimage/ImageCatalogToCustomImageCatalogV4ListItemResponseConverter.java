package com.sequenceiq.cloudbreak.converter.v4.customimage;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ListItemResponse;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class ImageCatalogToCustomImageCatalogV4ListItemResponseConverter  {

    public CustomImageCatalogV4ListItemResponse convert(ImageCatalog source) {
        CustomImageCatalogV4ListItemResponse result = new CustomImageCatalogV4ListItemResponse();
        result.setName(source.getName());
        result.setDescription(source.getDescription());

        return result;
    }
}
