package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.base.ImageCatalogV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ImageCatalogV4Request extends ImageCatalogV4Base {
}
