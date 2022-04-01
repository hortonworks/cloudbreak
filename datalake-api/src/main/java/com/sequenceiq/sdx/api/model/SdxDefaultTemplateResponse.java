package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDefaultTemplateResponse {

    @ApiModelProperty(ModelDescriptions.TEMPLATE_DETAILS)
    private StackV4Request template;

    public SdxDefaultTemplateResponse() {
    }

    public SdxDefaultTemplateResponse(StackV4Request template) {
        this.template = template;
    }

    public StackV4Request getTemplate() {
        return template;
    }

    public void setTemplate(StackV4Request template) {
        this.template = template;
    }

    @Override
    public String toString() {
        return "SdxDefaultTemplateResponse{" +
                "template=" + template +
                '}';
    }
}
