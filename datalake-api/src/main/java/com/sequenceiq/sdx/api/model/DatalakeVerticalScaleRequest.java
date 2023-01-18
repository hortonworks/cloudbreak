package com.sequenceiq.sdx.api.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatalakeVerticalScaleRequest implements JsonEntity {

    @NotNull
    @Schema(description = ModelDescriptions.HOST_GROUP_NAME)
    private String group;

    @Valid
    @NotNull
    @Schema(description = ModelDescriptions.CUSTOM_INSTANCE_GROUP_OPTIONS)
    private InstanceTemplateV4Request template;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public InstanceTemplateV4Request getInstanceTemplateV4Request() {
        return template;
    }

    public void setInstanceTemplateV4Request(InstanceTemplateV4Request template) {
        this.template = template;
    }
}
