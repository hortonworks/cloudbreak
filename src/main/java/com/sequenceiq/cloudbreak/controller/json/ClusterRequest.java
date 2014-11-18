package com.sequenceiq.cloudbreak.controller.json;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ClusterRequest {

    @Size(max = 100, min = 5, message = "Name has to be min 5 letter maximum 50 length")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "Must contain only alphanumeric characters (case sensitive) and hyphens and start with an alpha character.")
    private String name;
    private Long blueprintId;
    private String description;
    private Boolean emailNeeded = Boolean.FALSE;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Boolean getEmailNeeded() {
        return emailNeeded;
    }

    public void setEmailNeeded(Boolean emailNeeded) {
        this.emailNeeded = emailNeeded;
    }
}
