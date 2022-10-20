package com.sequenceiq.cloudbreak.converter.v4.customimage;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteImageResponse;
import com.sequenceiq.cloudbreak.domain.CustomImage;

@Component
public class CustomImageToCustomImageCatalogV4DeleteImageResponseConverter {

    public CustomImageCatalogV4DeleteImageResponse convert(CustomImage source) {
        CustomImageCatalogV4DeleteImageResponse result = new CustomImageCatalogV4DeleteImageResponse();
        result.setImageId(source.getName());

        return result;
    }
}
