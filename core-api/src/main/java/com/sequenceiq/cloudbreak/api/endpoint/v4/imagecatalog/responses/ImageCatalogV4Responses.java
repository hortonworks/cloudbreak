package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(Include.NON_NULL)
@NotNull
public class ImageCatalogV4Responses {

    private Set<ImageCatalogV4Response> catalogs;

    public Set<ImageCatalogV4Response> getCatalogs() {
        return catalogs;
    }

    public void setCatalogs(Set<ImageCatalogV4Response> catalogs) {
        this.catalogs = catalogs;
    }

    public static final ImageCatalogV4Responses imageCatalogResponses(Set<ImageCatalogV4Response> catalogs) {
        ImageCatalogV4Responses imageCatalogV4Responses = new ImageCatalogV4Responses();
        imageCatalogV4Responses.setCatalogs(catalogs);
        return imageCatalogV4Responses;
    }
}
