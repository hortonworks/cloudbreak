package com.sequenceiq.cloudbreak.converter.v4.customimage;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateResponse;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@Component
public class ImageCatalogToCustomImageCatalogV4CreateResponseConverter {

    public CustomImageCatalogV4CreateResponse convert(ImageCatalog source) {
        CustomImageCatalogV4CreateResponse result = new CustomImageCatalogV4CreateResponse();
        result.setName(source.getName());
        result.setDescription(source.getDescription());

        return result;
    }
}
