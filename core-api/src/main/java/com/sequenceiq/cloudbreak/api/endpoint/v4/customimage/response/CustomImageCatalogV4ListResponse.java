package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
@NotNull
public class CustomImageCatalogV4ListResponse extends GeneralCollectionV4Response<CustomImageCatalogV4ListItemResponse> {

    public CustomImageCatalogV4ListResponse(Set<CustomImageCatalogV4ListItemResponse> responses) {
        super(responses);
    }

    public CustomImageCatalogV4ListResponse() {
        super(Sets.newHashSet());
    }
}
