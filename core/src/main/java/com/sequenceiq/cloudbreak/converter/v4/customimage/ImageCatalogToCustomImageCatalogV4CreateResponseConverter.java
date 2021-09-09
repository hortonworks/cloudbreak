package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateResponse;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.springframework.stereotype.Component;

@Component
public class ImageCatalogToCustomImageCatalogV4CreateResponseConverter {

    public CustomImageCatalogV4CreateResponse convert(ImageCatalog source) {
        CustomImageCatalogV4CreateResponse result = new CustomImageCatalogV4CreateResponse();
        result.setName(source.getName());
        result.setDescription(source.getDescription());

        return result;
    }
}
