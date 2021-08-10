package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.identitymanagement.model.ContextEntry;

public class RequiredAction {
    private String resourceArn;

    private List<String> actionNames = new ArrayList<>();

    private List<ContextEntry> conditions = new ArrayList<>();

    public RequiredAction() { }

    public String getResourceArn() {
        return resourceArn;
    }

    public void setResourceArn(String resourceArn) {
        this.resourceArn = resourceArn;
    }

    public void setConditions(List<ContextEntry> conditions) {
        this.conditions = conditions;
    }

    public List<ContextEntry> getConditions() {
        return conditions;
    }

    public List<String> getActionNames() {
        return actionNames;
    }

    public void setActionNames(List<String> actionNames) {
        this.actionNames = actionNames;
    }
}
