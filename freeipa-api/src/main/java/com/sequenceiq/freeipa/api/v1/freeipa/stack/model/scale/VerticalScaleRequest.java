package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerticalScaleRequest implements JsonEntity {

    @NotNull
    private String group;

    @Valid
    @NotNull
    private InstanceTemplateRequest template;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public InstanceTemplateRequest getTemplate() {
        return template;
    }

    public void setTemplate(InstanceTemplateRequest template) {
        this.template = template;
    }

    @Override
    public String toString() {
        return super.toString() + "VerticalScaleRequest{" +
                "group='" + group + '\'' +
                ", template=" + template +
                '}';
    }
}
