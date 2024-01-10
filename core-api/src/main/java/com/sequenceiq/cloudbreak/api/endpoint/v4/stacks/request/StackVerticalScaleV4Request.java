package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackVerticalScaleV4Request implements JsonEntity {

    @NotNull
    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    private String group;

    @Valid
    @NotNull
    @Schema(description = InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateV4Request template;

    private Long stackId;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @JsonIgnore
    public Long getStackId() {
        return stackId;
    }

    @JsonIgnore
    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public InstanceTemplateV4Request getTemplate() {
        return template;
    }

    public void setTemplate(InstanceTemplateV4Request template) {
        this.template = template;
    }
}
