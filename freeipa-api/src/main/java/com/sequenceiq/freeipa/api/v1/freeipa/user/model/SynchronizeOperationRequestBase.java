package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public class SynchronizeOperationRequestBase {
    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_OPTIONAL_ENVIRONMENT_CRNS)
    private Set<String> environments = new HashSet<>();

    public SynchronizeOperationRequestBase() {
    }

    public SynchronizeOperationRequestBase(Set<String> environments) {
        this.environments = environments;
    }

    public Set<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<String> environments) {
        this.environments = environments;
    }

    @Override
    public String toString() {
        return "SynchronizeOperationRequestBase{"
                + "environments=" + environments
                + '}';
    }

    protected String fieldsToString() {
        return "environments=" + environments;
    }
}
