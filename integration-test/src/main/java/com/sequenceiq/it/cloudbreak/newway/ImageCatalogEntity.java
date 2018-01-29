package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;

public class ImageCatalogEntity extends AbstractCloudbreakEntity<ImageCatalogRequest, ImageCatalogResponse> {
    public static final String IMAGE_CATALOG = "IMAGE_CATALOG";

    ImageCatalogEntity(String newId) {
        super(newId);
        setRequest(new ImageCatalogRequest());
    }

    ImageCatalogEntity() {
        this(IMAGE_CATALOG);
    }

    public ImageCatalogEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ImageCatalogEntity withUrl(String url) {
        getRequest().setUrl(url);
        return this;
    }
}
