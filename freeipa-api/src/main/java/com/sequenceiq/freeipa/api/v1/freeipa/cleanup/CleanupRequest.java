package com.sequenceiq.freeipa.api.v1.freeipa.cleanup;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.authorization.api.EnvironmentCrnAwareApiModel;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CleanupV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CleanupRequest implements EnvironmentCrnAwareApiModel {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    private Set<String> users;

    private Set<String> hosts;

    private Set<String> roles;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public void setHosts(Set<String> hosts) {
        this.hosts = hosts;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "CleanupRequest{"
                + "environmentCrn='" + environmentCrn + '\''
                + ", users=" + users
                + ", hosts=" + hosts
                + ", roles=" + roles
                + '}';
    }
}
