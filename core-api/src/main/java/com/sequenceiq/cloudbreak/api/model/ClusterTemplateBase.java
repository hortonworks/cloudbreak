package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterTemplateModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class ClusterTemplateBase implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = ClusterTemplateModelDescription.NAME, required = true)
    private String name;

    @ApiModelProperty(ClusterTemplateModelDescription.TEMPLATE)
    private String template;

    @ApiModelProperty(ClusterTemplateModelDescription.TYPE)
    private ClusterTemplateType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonRawValue
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public ClusterTemplateType getType() {
        return type;
    }

    public void setType(ClusterTemplateType type) {
        this.type = type;
    }
}
