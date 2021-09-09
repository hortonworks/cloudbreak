package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteResponse;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.springframework.stereotype.Component;

@Component
public class ImageCatalogToCustomImageCatalogV4DeleteResponseConverter {

    public CustomImageCatalogV4DeleteResponse convert(ImageCatalog source) {
        CustomImageCatalogV4DeleteResponse result = new CustomImageCatalogV4DeleteResponse();
        result.setName(source.getName());

        return result;
    }
}
