package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidHttpContentSize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class BlueprintV4Request extends BlueprintV4Base {

    @ValidHttpContentSize
    @ApiModelProperty(BlueprintModelDescription.URL)
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
