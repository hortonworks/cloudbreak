package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackTemplate {

    private String template;

    private String cbVersion;

    public StackTemplate() {
    }

    public StackTemplate(String template, String cbVersion) {
        this.template = template;
        this.cbVersion = cbVersion;
    }

    public String getTemplate() {
        return template;
    }

    public String getCbVersion() {
        return cbVersion;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setCbVersion(String cbVersion) {
        this.cbVersion = cbVersion;
    }
}
