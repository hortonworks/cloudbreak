package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class ClusterTemplateBase implements JsonEntity {

    private String name;
    private String template;
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
