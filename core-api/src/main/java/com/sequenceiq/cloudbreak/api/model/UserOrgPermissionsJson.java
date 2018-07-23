package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel()
public class UserOrgPermissionsJson implements JsonEntity {

    private Set<String> permissions;

    private String userName;

    private String email;

    private String company;

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}
