package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.resource.AuthorizationFilterableResponseCollection;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(Include.NON_NULL)
@NotNull
public class ImageCatalogV4Responses extends GeneralCollectionV4Response<ImageCatalogV4Response>
        implements AuthorizationFilterableResponseCollection<ImageCatalogV4Response> {
    public ImageCatalogV4Responses(Set<ImageCatalogV4Response> responses) {
        super(responses);
    }

    public ImageCatalogV4Responses() {
        super(Sets.newHashSet());
    }
}
