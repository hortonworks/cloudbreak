package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserProfileV4Response {

    private String username;

    private String userId;

    private String tenant;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
