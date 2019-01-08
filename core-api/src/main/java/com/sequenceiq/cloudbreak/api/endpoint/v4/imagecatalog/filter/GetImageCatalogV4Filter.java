package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.filter;

import javax.ws.rs.QueryParam;

public class GetImageCatalogV4Filter {

    @QueryParam("withImages")
    private Boolean withImages;

    public Boolean getWithImages() {
        return withImages;
    }

    public void setWithImages(Boolean withImages) {
        this.withImages = withImages;
    }
}
