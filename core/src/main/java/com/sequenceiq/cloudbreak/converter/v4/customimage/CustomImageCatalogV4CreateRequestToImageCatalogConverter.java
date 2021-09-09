package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4CreateRequest;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.springframework.stereotype.Component;

@Component
public class CustomImageCatalogV4CreateRequestToImageCatalogConverter {

    public ImageCatalog convert(CustomImageCatalogV4CreateRequest source) {
        ImageCatalog result = new ImageCatalog();
        result.setName(source.getName());
        result.setDescription(source.getDescription());

        return result;
    }
}
