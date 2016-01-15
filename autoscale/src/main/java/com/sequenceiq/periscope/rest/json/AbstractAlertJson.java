package com.sequenceiq.periscope.rest.json;

import javax.validation.constraints.Pattern;

public abstract class AbstractAlertJson implements Json {

    private Long id;
    @Pattern(regexp = "([a-zA-Z][-a-zA-Z0-9]*)",
            message = "The name can only contain alphanumeric characters and hyphens and has start with an alphanumeric character")
    private String alertName;
    private String description;
    private Long scalingPolicyId;

    public String getAlertName() {
        return alertName;
    }

    public void setAlertName(String alertName) {
        this.alertName = alertName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScalingPolicyId() {
        return scalingPolicyId;
    }

    public void setScalingPolicyId(Long scalingPolicyId) {
        this.scalingPolicyId = scalingPolicyId;
    }

}
